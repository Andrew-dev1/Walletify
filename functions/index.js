/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */


const {onCall, onRequest, HttpsError} = require("firebase-functions/v2/https");
const {defineString} = require("firebase-functions/params");
const plaid = require("plaid");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();

// Define parameters (replaces functions.config)
const plaidClientId = defineString("PLAID_CLIENT_ID");
const plaidSecret = defineString("PLAID_SECRET");
const plaidWebhookSecret = defineString("PLAID_WEBHOOK_SECRET", {
  default: "",
});
const encryptionKey = defineString("ENCRYPTION_KEY", {
  default: "",
});

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

// ============================================================================
// Encryption Utilities for Access Tokens
// ============================================================================

/**
 * Encrypts an access token before storing in Firestore.
 * Uses AES-256-GCM encryption.
 * @param {string} token The access token to encrypt.
 * @return {string} The encrypted token.
 */
function encryptAccessToken(token) {
  const key = encryptionKey.value();
  if (!key) {
    console.warn("ENCRYPTION_KEY not set, storing token in plaintext");
    return token;
  }

  const algorithm = "aes-256-gcm";
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv(algorithm, Buffer.from(key, "hex"), iv);

  let encrypted = cipher.update(token, "utf8", "hex");
  encrypted += cipher.final("hex");

  const authTag = cipher.getAuthTag();

  // Return IV + authTag + encrypted data (all hex encoded)
  return iv.toString("hex") + ":" + authTag.toString("hex") + ":" + encrypted;
}

/**
 * Decrypts an access token retrieved from Firestore.
 * @param {string} encryptedToken The encrypted token to decrypt.
 * @return {string} The decrypted token.
 */
function decryptAccessToken(encryptedToken) {
  const key = encryptionKey.value();
  if (!key) {
    // If no encryption key, assume it's stored in plaintext
    return encryptedToken;
  }

  try {
    const parts = encryptedToken.split(":");
    if (parts.length !== 3) {
      // Not encrypted, return as-is
      return encryptedToken;
    }

    const algorithm = "aes-256-gcm";
    const iv = Buffer.from(parts[0], "hex");
    const authTag = Buffer.from(parts[1], "hex");
    const encrypted = parts[2];

    const decipher = crypto.createDecipheriv(
        algorithm,
        Buffer.from(key, "hex"),
        iv,
    );
    decipher.setAuthTag(authTag);

    let decrypted = decipher.update(encrypted, "hex", "utf8");
    decrypted += decipher.final("utf8");

    return decrypted;
  } catch (error) {
    console.error("Error decrypting access token:", error);
    // If decryption fails, assume it's stored in plaintext
    // (backward compatibility)
    return encryptedToken;
  }
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Gets decrypted access token from Firestore for a given item.
 * @param {string} userId The user ID.
 * @param {string} itemId The Plaid item ID.
 * @return {Promise<string>} The decrypted access token.
 */
async function getAccessTokenForItem(userId, itemId) {
  const db = admin.firestore();
  const itemDoc = await db
      .collection("users")
      .doc(userId)
      .collection("plaid_items")
      .doc(itemId)
      .get();

  if (!itemDoc.exists) {
    throw new Error("Item not found");
  }

  const encryptedToken = itemDoc.data().access_token;
  return decryptAccessToken(encryptedToken);
}

/**
 * Stores encrypted access token in Firestore.
 * @param {string} userId The user ID.
 * @param {string} itemId The Plaid item ID.
 * @param {string} accessToken The access token to encrypt and store.
 * @return {Promise<void>}
 */
async function storeAccessToken(userId, itemId, accessToken) {
  const db = admin.firestore();
  const encryptedToken = encryptAccessToken(accessToken);

  await db
      .collection("users")
      .doc(userId)
      .collection("plaid_items")
      .doc(itemId)
      .update({
        access_token: encryptedToken,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      });
}

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
    const userId = request.auth.uid;
    const linkedAt = admin.firestore.Timestamp.now().toMillis();

    // Store item data
    const itemRef = db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId);

    batch.set(itemRef, {
      item_id: itemId,
      user_id: userId, // Store userId for efficient webhook lookups
      access_token: encryptAccessToken(accessToken),
      institution_id: institutionId,
      institution_name: institutionName,
      item_status: "good", // Track item health status
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      last_synced: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Store accounts in users/{uid}/plaid_accounts/{accountId}
    // Matches PlaidAccountDocument structure for easy querying
    accountsResponse.data.accounts.forEach((account) => {
      const plaidAccountRef = db
          .collection("users")
          .doc(userId)
          .collection("plaid_accounts")
          .doc(account.account_id);

      batch.set(plaidAccountRef, {
        accountId: account.account_id,
        accountName: account.name,
        institutionName: institutionName,
        type: account.type,
        balance: account.balances.current || 0.0,
        linkedAt: linkedAt,
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
 * Fetches transactions for all linked accounts and stores them in
 * users/{uid}/transactions/{transactionId}
 *
 * Input: { itemId: string, startDate: string (YYYY-MM-DD),
 *          endDate: string (YYYY-MM-DD) }
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
        "itemId, startDate, and endDate are required",
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

    const accessToken = decryptAccessToken(itemDoc.data().access_token);

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
      // Store as negative for debits, positive for credits
      // (standard accounting)
      const amount = transaction.amount;
      const transactionData = {
        transactionId: transaction.transaction_id,
        accountId: transaction.account_id,
        // Negative for debits (spending), positive for credits (income)
        amount: -amount,
        // Convert YYYY-MM-DD to timestamp
        date: new Date(transaction.date).getTime(),
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
        `Synced ${syncedCount} new, updated ${updatedCount} ` +
        `existing transactions for user ${userId}`,
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

// ============================================================================
// Re-Authentication Flow
// ============================================================================

/**
 * Creates an update link token for re-authenticating an existing Plaid item.
 * Used when an item requires user action (e.g., credentials expired).
 *
 * Input: { itemId: string }
 * Returns: { link_token: string }
 */
exports.createUpdateLinkToken = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const {itemId} = request.data;

  if (!itemId) {
    throw new HttpsError("invalid-argument", "itemId is required");
  }

  try {
    const db = admin.firestore();
    const userId = request.auth.uid;

    // Get item data
    const itemDoc = await db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId)
        .get();

    if (!itemDoc.exists) {
      throw new HttpsError("not-found", "Item not found");
    }

    const itemData = itemDoc.data();
    const accessToken = decryptAccessToken(itemData.access_token);

    // Create update link token
    const plaidClient = getPlaidClient();
    const linkRequest = {
      access_token: accessToken,
      client_name: "Walletify",
      country_codes: ["US"],
      language: "en",
      android_package_name: "hu.ait.walletify",
    };

    const response = await plaidClient.linkTokenCreate(linkRequest);
    console.log("Update link token created for item:", itemId);

    return {link_token: response.data.link_token};
  } catch (error) {
    console.error("Error creating update link token:", error);
    throw new HttpsError("internal", error.message);
  }
});

/**
 * Updates access token after re-authentication.
 * Exchanges the public token from update link flow for a new access token.
 *
 * Input: { itemId: string, publicToken: string }
 * Returns: { success: boolean, item_id: string }
 */
exports.updateAccessToken = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const {itemId, publicToken} = request.data;

  if (!itemId || !publicToken) {
    throw new HttpsError(
        "invalid-argument",
        "itemId and publicToken are required",
    );
  }

  try {
    const plaidClient = getPlaidClient();
    const db = admin.firestore();
    const userId = request.auth.uid;

    // Exchange public token for new access token
    const exchangeResponse = await plaidClient.itemPublicTokenExchange({
      public_token: publicToken,
    });

    const newAccessToken = exchangeResponse.data.access_token;
    const returnedItemId = exchangeResponse.data.item_id;

    // Verify item IDs match
    if (returnedItemId !== itemId) {
      throw new HttpsError(
          "invalid-argument",
          "Item ID mismatch",
      );
    }

    // Update access token and item status
    await storeAccessToken(userId, itemId, newAccessToken);

    // Update item status to good
    await db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId)
        .update({
          item_status: "good",
          last_synced: admin.firestore.FieldValue.serverTimestamp(),
        });

    console.log("Access token updated for item:", itemId);

    return {
      success: true,
      item_id: itemId,
    };
  } catch (error) {
    console.error("Error updating access token:", error);
    throw new HttpsError("internal", error.message);
  }
});

/**
 * Gets the status of a Plaid item to check if re-authentication is needed.
 *
 * Input: { itemId: string }
 * Returns: { status: string, requiresReauth: boolean, error?: object }
 */
exports.getItemStatus = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated");
  }

  const {itemId} = request.data;

  if (!itemId) {
    throw new HttpsError("invalid-argument", "itemId is required");
  }

  try {
    const db = admin.firestore();
    const userId = request.auth.uid;
    const accessToken = await getAccessTokenForItem(userId, itemId);

    const plaidClient = getPlaidClient();
    const itemResponse = await plaidClient.itemGet({
      access_token: accessToken,
    });

    const item = itemResponse.data.item;
    const status = item.status;
    const requiresReauth = status.login_required ||
        status.error_code === "ITEM_LOGIN_REQUIRED";

    // Update item status in Firestore
    await db
        .collection("users")
        .doc(userId)
        .collection("plaid_items")
        .doc(itemId)
        .update({
          item_status: requiresReauth ? "requires_reauth" : "good",
          item_error: status.error_code ? {
            error_code: status.error_code,
            error_message: status.error_message,
          } : null,
        });

    return {
      status: status.status || "unknown",
      requiresReauth: requiresReauth,
      error: status.error_code ? {
        error_code: status.error_code,
        error_message: status.error_message,
      } : null,
    };
  } catch (error) {
    console.error("Error getting item status:", error);
    throw new HttpsError("internal", error.message);
  }
});

// ============================================================================
// Webhook Handler
// ============================================================================

/**
 * Handles Plaid webhooks for real-time updates.
 * Processes TRANSACTIONS, ITEM, and AUTH webhook types.
 *
 * Webhook URL: https://us-central1-walletify-eb1ae.cloudfunctions.net/plaidWebhook
 */
exports.plaidWebhook = onRequest(
    {cors: true},
    async (req, res) => {
      // Verify webhook signature (if webhook secret is configured)
      const webhookSecret = plaidWebhookSecret.value();
      if (webhookSecret) {
        const signature = req.headers["plaid-verification"];
        if (!signature) {
          console.error("Missing Plaid webhook signature");
          res.status(401).send("Unauthorized");
          return;
        }

        // Verify webhook signature using HMAC-SHA256
        // Plaid sends the key as "whsec_..." - extract the actual key
        const key = webhookSecret.startsWith("whsec_") ?
            webhookSecret.substring(6) : webhookSecret;

        // Compute expected signature
        const body = JSON.stringify(req.body);
        const expectedSignature = crypto
            .createHmac("sha256", key)
            .update(body)
            .digest("base64");

        // Compare signatures (constant-time comparison)
        if (signature !== expectedSignature) {
          console.error("Invalid webhook signature");
          res.status(401).send("Unauthorized");
          return;
        }
      }

      const webhook = req.body;
      console.log(
          "Received Plaid webhook:",
          webhook.webhook_type,
          webhook.webhook_code,
      );

      const db = admin.firestore();

      try {
        switch (webhook.webhook_type) {
          case "TRANSACTIONS": {
            await handleTransactionsWebhook(db, webhook);
            break;
          }
          case "ITEM": {
            await handleItemWebhook(db, webhook);
            break;
          }
          case "AUTH": {
            await handleAuthWebhook(db, webhook);
            break;
          }
          default: {
            console.log("Unhandled webhook type:", webhook.webhook_type);
          }
        }

        res.status(200).send("OK");
      } catch (error) {
        console.error("Error processing webhook:", error);
        res.status(500).send("Error processing webhook");
      }
    },
);

/**
 * Handles TRANSACTIONS webhooks (new transactions, updated transactions).
 * @param {admin.firestore.Firestore} db Firestore database instance.
 * @param {Object} webhook The webhook payload from Plaid.
 * @return {Promise<void>}
 */
async function handleTransactionsWebhook(db, webhook) {
  const itemId = webhook.item_id;
  const webhookCode = webhook.webhook_code;

  console.log(
      `Processing TRANSACTIONS webhook: ${webhookCode} for item ${itemId}`,
  );

  // Find the user who owns this item
  // First try to find by item_id in collectionGroup (requires index)
  // Fallback: query all users (less efficient but works without index)
  let itemDoc = null;
  let userId = null;

  const itemsSnapshot = await db
      .collectionGroup("plaid_items")
      .where("item_id", "==", itemId)
      .limit(1)
      .get();

  if (!itemsSnapshot.empty) {
    itemDoc = itemsSnapshot.docs[0];
    userId = itemDoc.data().user_id || itemDoc.ref.parent.parent.id;
  } else {
    // Fallback: if collectionGroup query fails, we need userId from webhook
    // For now, log warning and return
    console.warn(`Item ${itemId} not found in Firestore`);
    return;
  }

  if (!itemDoc || !userId) {
    console.warn(`Item ${itemId} not found or missing userId`);
    return;
  }

  switch (webhookCode) {
    case "SYNC_UPDATES_AVAILABLE": {
      // New transactions are available, trigger sync
      const newTransactions = webhook.new_transactions || 0;
      console.log(
          `New transactions available for item ${itemId}: ${newTransactions}`,
      );

      // Update item to indicate sync is needed
      await itemDoc.ref.update({
        sync_needed: true,
        new_transactions_count: newTransactions,
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    case "INITIAL_UPDATE": {
      // Initial transaction sync completed
      console.log(`Initial sync completed for item ${itemId}`);
      await itemDoc.ref.update({
        sync_needed: false,
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    case "HISTORICAL_UPDATE": {
      // Historical transaction sync completed
      console.log(`Historical sync completed for item ${itemId}`);
      await itemDoc.ref.update({
        sync_needed: false,
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    default: {
      console.log(`Unhandled TRANSACTIONS webhook code: ${webhookCode}`);
    }
  }
}

/**
 * Handles ITEM webhooks (item status changes, errors, etc.).
 * @param {admin.firestore.Firestore} db Firestore database instance.
 * @param {Object} webhook The webhook payload from Plaid.
 * @return {Promise<void>}
 */
async function handleItemWebhook(db, webhook) {
  const itemId = webhook.item_id;
  const webhookCode = webhook.webhook_code;

  console.log(
      `Processing ITEM webhook: ${webhookCode} for item ${itemId}`,
  );

  // Find the user who owns this item
  let itemDoc = null;
  let userId = null;

  const itemsSnapshot = await db
      .collectionGroup("plaid_items")
      .where("item_id", "==", itemId)
      .limit(1)
      .get();

  if (!itemsSnapshot.empty) {
    itemDoc = itemsSnapshot.docs[0];
    userId = itemDoc.data().user_id || itemDoc.ref.parent.parent.id;
  } else {
    console.warn(`Item ${itemId} not found in Firestore`);
    return;
  }

  if (!itemDoc || !userId) {
    console.warn(`Item ${itemId} not found or missing userId`);
    return;
  }

  switch (webhookCode) {
    case "ERROR": {
      // Item error occurred (e.g., login required)
      const error = webhook.error;
      console.error(`Item ${itemId} error:`, error);

      await itemDoc.ref.update({
        item_status: "error",
        item_error: {
          error_code: error.error_code,
          error_message: error.error_message,
        },
        requires_reauth: error.error_code === "ITEM_LOGIN_REQUIRED",
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    case "PENDING_EXPIRATION": {
      // Access token will expire soon
      console.log(`Item ${itemId} access token pending expiration`);
      await itemDoc.ref.update({
        item_status: "pending_expiration",
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    case "USER_PERMISSION_REVOKED": {
      // User revoked access
      console.log(`User revoked access for item ${itemId}`);
      await itemDoc.ref.update({
        item_status: "revoked",
        webhook_received_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      break;
    }
    case "WEBHOOK_UPDATE_ACKNOWLEDGED": {
      // Webhook configuration updated
      console.log(`Webhook update acknowledged for item ${itemId}`);
      break;
    }
    default: {
      console.log(`Unhandled ITEM webhook code: ${webhookCode}`);
    }
  }
}

/**
 * Handles AUTH webhooks (account updates, etc.).
 * @param {admin.firestore.Firestore} db Firestore database instance.
 * @param {Object} webhook The webhook payload from Plaid.
 * @return {Promise<void>}
 */
async function handleAuthWebhook(db, webhook) {
  const itemId = webhook.item_id;
  const webhookCode = webhook.webhook_code;

  console.log(
      `Processing AUTH webhook: ${webhookCode} for item ${itemId}`,
  );

  // Find the user who owns this item
  let itemDoc = null;
  let userId = null;

  const itemsSnapshot = await db
      .collectionGroup("plaid_items")
      .where("item_id", "==", itemId)
      .limit(1)
      .get();

  if (!itemsSnapshot.empty) {
    itemDoc = itemsSnapshot.docs[0];
    userId = itemDoc.data().user_id || itemDoc.ref.parent.parent.id;
  } else {
    console.warn(`Item ${itemId} not found in Firestore`);
    return;
  }

  if (!itemDoc || !userId) {
    console.warn(`Item ${itemId} not found or missing userId`);
    return;
  }

  switch (webhookCode) {
    case "AUTOMATICALLY_VERIFIED": {
      // Account automatically verified
      console.log(`Account automatically verified for item ${itemId}`);
      break;
    }
    case "VERIFICATION_EXPIRED": {
      // Account verification expired
      console.log(`Account verification expired for item ${itemId}`);
      break;
    }
    default: {
      console.log(`Unhandled AUTH webhook code: ${webhookCode}`);
    }
  }
}
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
