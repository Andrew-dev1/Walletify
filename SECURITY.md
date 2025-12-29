# Security Guide - Files to Keep Private

## ⚠️ CRITICAL: Files That MUST NOT Be Committed

### 1. `local.properties` - **CONTAINS PLAID SECRETS!**
**Status:** Currently contains your Plaid API credentials
- `PLAID_CLIENT_ID=6929ae78111b5200219bca18`
- `PLAID_SECRET=ee00bf3428c45b58869508b862cf7e`

**Action Required:**
1. If you've already committed this file, **rotate your Plaid API keys immediately**
2. Go to https://dashboard.plaid.com/ and generate new keys
3. Update `local.properties` with new keys
4. The `.gitignore` now excludes this file

### 2. `app/google-services.json` - **CONTAINS FIREBASE CONFIG**
**Status:** Contains Firebase project configuration
- Project ID, API keys, etc.

**Action Required:**
1. If committed, it's less critical but still should be private
2. The `.gitignore` now excludes this file
3. Use `google-services.json.example` as a template for others

### 3. Firebase Functions Environment Variables
**Location:** Firebase Console → Functions → Configuration
- `PLAID_CLIENT_ID`
- `PLAID_SECRET`

**Action Required:**
- Set these in Firebase Console, not in code
- Never commit these values

## ✅ Files Safe to Commit

- Source code (`.kt`, `.js` files)
- `build.gradle.kts`
- `gradle.properties` (if no secrets)
- `firestore.rules`
- `firestore.indexes.json`
- `firebase.json`
- `README.md`
- `LICENSE`
- Example/template files (`.example`)

## 📋 Template Files Created

I've created template files you CAN commit:
- `local.properties.example` - Template for local.properties
- `app/google-services.json.example` - Template for Firebase config

These help other developers set up the project without exposing secrets.

## 🔒 If You've Already Committed Secrets

1. **Rotate all exposed API keys immediately**
2. Remove from git history:
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch local.properties app/google-services.json" \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. Force push (if already pushed to remote)
4. Update all team members

## ✅ Updated .gitignore

The `.gitignore` now properly excludes:
- ✅ `local.properties` (Plaid secrets)
- ✅ `google-services.json` (Firebase config)
- ✅ `functions/node_modules/` (Node.js dependencies)
- ✅ Build artifacts
- ✅ IDE files
- ✅ Environment files
- ✅ Keystore files



