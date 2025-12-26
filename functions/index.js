/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */


const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineString} = require("firebase-functions/params");
const plaid = require("plaid");
const admin = require("firebase-admin");

admin.initializeApp();

// Define parameters (replaces functions.config)
const plaidClientId = defineString("PLAID_CLIENT_ID");
const plaidSecret = defineString("PLAID_SECRET");

// Initialize Plaid client
const getPlaidClient = () => {
  return new plaid.PlaidApi(
      new plaid.Configuration({
        basePath: plaid.PlaidEnvironments.sandbox,
        baseOptions: {
          headers: {
            "PLAID-CLIENT-ID": plaidClientId.value(),
            "PLAID-SECRET": plaidSecret.value(),
          },
        },
      }),
  );
};

exports.testing = onCall(async (request) => {
  return "hello";
});


// Create link token
exports.createLinkToken = onCall(async (request) => {
  console.log("=== createLinkToken called ===");
  console.log("Auth object:", JSON.stringify(request.auth, null, 2));
  console.log("Data:", JSON.stringify(request.data, null, 2));

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const linkRequest = {
    user: {client_user_id: request.auth.uid},
    client_name: "Walletify",
    products: ["auth", "transactions"],
    country_codes: ["US"],
    language: "en",
    android_package_name: "hu.ait.walletify",
  };

  try {
    const plaidClient = getPlaidClient();
    const response = await plaidClient.linkTokenCreate(linkRequest);
    console.log("Link token created:", response.data.link_token);

    return {link_token: response.data.link_token};
  } catch (error) {
    console.error("Error creating link token:", error);
    throw new HttpsError("internal", error.message);
  }
});

// Exchange public token
exports.exchangePublicToken = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const {publicToken} = request.data;

  if (!publicToken) {
    throw new HttpsError("invalid-argument", "publicToken is required");
  }

  try {
    const plaidClient = getPlaidClient();

    // Exchange public token for access token
    const exchangeResponse = await plaidClient.itemPublicTokenExchange({
      public_token: publicToken,
    });

    const accessToken = exchangeResponse.data.access_token;
    const itemId = exchangeResponse.data.item_id;

    // Get institution info
    const itemResponse = await plaidClient.itemGet({
      access_token: accessToken,
    });

    const institutionId = itemResponse.data.item.institution_id;

    // Get institution name
    const institutionResponse = await plaidClient.institutionsGetById({
      institution_id: institutionId,
      country_codes: ["US"],
    });

    const institutionName = institutionResponse.data.institution.name;

    // Get accounts
    const accountsResponse = await plaidClient.accountsGet({
      access_token: accessToken,
    });

    const db = admin.firestore();
    const batch = db.batch();

    // Store item data
    const itemRef = db
        .collection("users")
        .doc(request.auth.uid)
        .collection("plaid_items")
        .doc(itemId);

    batch.set(itemRef, {
      item_id: itemId,
      access_token: accessToken, // Consider encrypting this!
      institution_id: institutionId,
      institution_name: institutionName,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      last_synced: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Store accounts
    accountsResponse.data.accounts.forEach((account) => {
      const accountRef = itemRef
          .collection("accounts")
          .doc(account.account_id);

      batch.set(accountRef, {
        account_id: account.account_id,
        name: account.name,
        type: account.type,
        subtype: account.subtype,
        mask: account.mask,
        current_balance: account.balances.current,
        available_balance: account.balances.available,
        currency: account.balances.iso_currency_code,
      });
    });

    await batch.commit();

    return {
      item_id: itemId,
      success: true,
      institution_name: institutionName,
      accounts_count: accountsResponse.data.accounts.length,
    };
  } catch (error) {
    console.error("Error exchanging public token:", error);
    throw new HttpsError("internal", error.message);
  }
});



/**
 * Sync transactions from Plaid and store in Firestore.
 * Fetches transactions for all linked accounts and stores them in users/{uid}/transactions/{transactionId}
 * 
 * Input: { itemId: string, startDate: string (YYYY-MM-DD), endDate: string (YYYY-MM-DD) }
 * Returns: { synced: number, updated: number, total: number }
 */
exports.syncTransactions = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const {itemId, startDate, endDate} = request.data;

  if (!itemId || !startDate || !endDate) {
    throw new HttpsError(
        "invalid-argument",
        "itemId, startDate, and endDate are required"
    );
  }

  try {
    const db = admin.firestore();
    const userId = request.auth.uid;

    // Get access token from Firestore
    const itemDoc = await db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId)
        .get();

    if (!itemDoc.exists) {
      throw new HttpsError("not-found", "Item not found");
    }

    const accessToken = itemDoc.data().access_token;

    // Fetch transactions from Plaid
    const plaidClient = getPlaidClient();
    const response = await plaidClient.transactionsGet({
      access_token: accessToken,
      start_date: startDate,
      end_date: endDate,
    });

    const transactions = response.data.transactions;
    const batch = db.batch();
    const now = admin.firestore.Timestamp.now().toMillis();

    let syncedCount = 0;
    let updatedCount = 0;

    // Process each transaction
    for (const transaction of transactions) {
      const transactionRef = db
          .collection("users")
          .doc(userId)
          .collection("transactions")
          .doc(transaction.transaction_id);

      // Check if transaction already exists
      const existingDoc = await transactionRef.get();

      // Convert Plaid transaction to Firestore document format
      // Plaid amount is positive for debits, negative for credits
      // We'll store as negative for debits, positive for credits (standard accounting)
      const amount = transaction.amount;
      const transactionData = {
        transactionId: transaction.transaction_id,
        accountId: transaction.account_id,
        amount: -amount, // Negative for debits (spending), positive for credits (income)
        date: new Date(transaction.date).getTime(), // Convert YYYY-MM-DD to timestamp
        name: transaction.name || transaction.merchant_name || "Unknown",
        category: transaction.category || ["Other"],
        pending: transaction.pending || false,
        merchantName: transaction.merchant_name || null,
        syncedAt: now,
      };

      if (existingDoc.exists) {
        // Update existing transaction
        batch.update(transactionRef, transactionData);
        updatedCount++;
      } else {
        // Create new transaction
        batch.set(transactionRef, transactionData);
        syncedCount++;
      }
    }

    // Commit all transactions
    await batch.commit();

    // Update last_synced timestamp on item
    await db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId)
        .update({
          last_synced: admin.firestore.FieldValue.serverTimestamp(),
        });

    console.log(
        `Synced ${syncedCount} new, updated ${updatedCount} existing transactions for user ${userId}`
    );

    return {
      synced: syncedCount,
      updated: updatedCount,
      total: transactions.length,
    };
  } catch (error) {
    console.error("Error syncing transactions:", error);
    throw new HttpsError("internal", error.message);
  }
});
// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
// setGlobalOptions({maxInstances: 10});

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
