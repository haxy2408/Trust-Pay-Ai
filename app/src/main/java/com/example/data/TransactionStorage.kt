package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AuditLogEntry
import com.example.model.AuditStatus
import com.example.model.PaymentTransaction
import com.example.model.UserAccount
import com.example.security.AuthSecurityUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local persistence manager backed by Android SharedPreferences.
 *
 * Security Rule 4:
 * Stores successful transaction history locally using SharedPreferences
 * so that killing or restarting the app cannot bypass the rolling 5-minute
 * velocity window rule.
 *
 * Security Rule 9:
 * Provides clearHistory() to purge transactions and reset demo environment.
 */
class TransactionStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_DEMO_BALANCE = 100000.0
        private const val PREFS_NAME = "trustpay_ai_secure_prefs"
        private const val KEY_TRANSACTIONS = "key_tx_history_json"
        private const val KEY_AUDIT_LOGS = "key_audit_logs_json"
        private const val KEY_DARK_THEME = "key_dark_security_theme"
        private const val KEY_USERS = "key_registered_users_json"
        private const val KEY_IS_LOGGED_IN = "key_user_is_logged_in"
        private const val KEY_ACTIVE_USER_EMAIL = "key_active_user_email"
        private const val KEY_DEMO_BALANCE_PREFIX = "key_demo_balance_"
        private const val KEY_GLOBAL_DEMO_BALANCE = "key_demo_balance_global"
    }

    /**
     * Retrieves the stored demo account balance.
     * Defaults to ₹1,00,000.00 if not previously set.
     */
    fun getAccountBalance(email: String? = getActiveUser()?.email): Double {
        val userKey = if (email != null) KEY_DEMO_BALANCE_PREFIX + email.trim().lowercase() else null
        if (userKey != null && prefs.contains(userKey)) {
            val balanceStr = prefs.getString(userKey, null)
            return balanceStr?.toDoubleOrNull() ?: DEFAULT_DEMO_BALANCE
        }
        if (prefs.contains(KEY_GLOBAL_DEMO_BALANCE)) {
            val balanceStr = prefs.getString(KEY_GLOBAL_DEMO_BALANCE, null)
            return balanceStr?.toDoubleOrNull() ?: DEFAULT_DEMO_BALANCE
        }
        // Initialize default balance
        setAccountBalance(DEFAULT_DEMO_BALANCE, email)
        return DEFAULT_DEMO_BALANCE
    }

    /**
     * Saves the demo account balance locally so it persists across restarts and logouts.
     */
    fun setAccountBalance(balance: Double, email: String? = getActiveUser()?.email) {
        val editor = prefs.edit()
        val balanceStr = balance.toString()
        editor.putString(KEY_GLOBAL_DEMO_BALANCE, balanceStr)
        if (email != null) {
            editor.putString(KEY_DEMO_BALANCE_PREFIX + email.trim().lowercase(), balanceStr)
        }
        editor.apply()
    }

    /**
     * Restores demo account balance to ₹1,00,000.00.
     */
    fun resetAccountBalance(email: String? = getActiveUser()?.email) {
        setAccountBalance(DEFAULT_DEMO_BALANCE, email)
    }

    /**
     * Retrieves all recorded successful transactions.
     */
    fun getTransactions(): List<PaymentTransaction> {
        val jsonStr = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val result = mutableListOf<PaymentTransaction>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val signalsJson = obj.optJSONArray("signals")
                val signalsList = mutableListOf<String>()
                if (signalsJson != null) {
                    for (j in 0 until signalsJson.length()) {
                        signalsList.add(signalsJson.getString(j))
                    }
                }

                val balanceAfter = if (obj.has("balanceAfter")) obj.getDouble("balanceAfter") else null

                result.add(
                    PaymentTransaction(
                        id = obj.getString("id"),
                        recipient = obj.getString("recipient"),
                        amount = obj.getDouble("amount"),
                        currency = obj.optString("currency", "INR"),
                        timestamp = obj.getLong("timestamp"),
                        payloadHash = obj.getString("payloadHash"),
                        status = obj.optString("status", "APPROVED"),
                        riskScore = obj.optInt("riskScore", 0),
                        signals = signalsList,
                        balanceAfter = balanceAfter
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Appends a successfully verified payment to local history.
     */
    fun saveTransaction(tx: PaymentTransaction) {
        val current = getTransactions().toMutableList()
        current.add(0, tx) // newest first

        try {
            val jsonArray = JSONArray()
            for (item in current) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("recipient", item.recipient)
                obj.put("amount", item.amount)
                obj.put("currency", item.currency)
                obj.put("timestamp", item.timestamp)
                obj.put("payloadHash", item.payloadHash)
                obj.put("status", item.status)
                obj.put("riskScore", item.riskScore)
                if (item.balanceAfter != null) {
                    obj.put("balanceAfter", item.balanceAfter)
                }

                val sigArray = JSONArray()
                for (sig in item.signals) {
                    sigArray.put(sig)
                }
                obj.put("signals", sigArray)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears all local transaction history.
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_TRANSACTIONS).apply()
    }

    /**
     * Retrieves audit logs.
     */
    fun getAuditLogs(): List<AuditLogEntry> {
        val jsonStr = prefs.getString(KEY_AUDIT_LOGS, null) ?: return emptyList()
        val result = mutableListOf<AuditLogEntry>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val statusStr = obj.optString("status", AuditStatus.INFO.name)
                val status = try {
                    AuditStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    AuditStatus.INFO
                }
                result.add(
                    AuditLogEntry(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        title = obj.getString("title"),
                        details = obj.getString("details"),
                        status = status,
                        hash = if (obj.has("hash")) obj.getString("hash") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Appends an audit log entry.
     */
    fun saveAuditLog(entry: AuditLogEntry) {
        val current = getAuditLogs().toMutableList()
        current.add(0, entry) // newest first
        // keep up to 100 entries
        val trimmed = if (current.size > 100) current.take(100) else current

        try {
            val jsonArray = JSONArray()
            for (item in trimmed) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("timestamp", item.timestamp)
                obj.put("title", item.title)
                obj.put("details", item.details)
                obj.put("status", item.status.name)
                if (item.hash != null) {
                    obj.put("hash", item.hash)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_AUDIT_LOGS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears audit logs.
     */
    fun clearAuditLogs() {
        prefs.edit().remove(KEY_AUDIT_LOGS).apply()
    }

    /**
     * Checks if high-contrast dark security mode is enabled.
     */
    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, false)
    }

    /**
     * Saves user theme preference.
     */
    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    /**
     * Stores a registered user account.
     * Note: Passwords must already be securely hashed with salt before passing to this method.
     */
    fun saveUser(user: UserAccount) {
        val existing = getUsers().toMutableList()
        // Replace existing user with same email if present, or append
        val index = existing.indexOfFirst { it.email.equals(user.email, ignoreCase = true) }
        if (index >= 0) {
            existing[index] = user
        } else {
            existing.add(user)
        }

        try {
            val jsonArray = JSONArray()
            for (u in existing) {
                val obj = JSONObject()
                obj.put("fullName", u.fullName)
                obj.put("email", u.email)
                obj.put("mobileNumber", u.mobileNumber)
                obj.put("passwordHash", u.passwordHash)
                obj.put("salt", u.salt)
                obj.put("createdAt", u.createdAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_USERS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves all locally registered user accounts.
     */
    fun getUsers(): List<UserAccount> {
        val jsonStr = prefs.getString(KEY_USERS, null) ?: return emptyList()
        val result = mutableListOf<UserAccount>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    UserAccount(
                        fullName = obj.getString("fullName"),
                        email = obj.getString("email"),
                        mobileNumber = obj.getString("mobileNumber"),
                        passwordHash = obj.getString("passwordHash"),
                        salt = obj.getString("salt"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Finds a user matching either their email (case-insensitive) or 10-digit mobile number.
     */
    fun findUserByIdentifier(identifier: String): UserAccount? {
        val cleanIdentifier = identifier.trim()
        val normalizedDigits = AuthSecurityUtil.extractNormalized10Digits(cleanIdentifier)
        val users = getUsers()

        return users.firstOrNull { user ->
            user.email.equals(cleanIdentifier, ignoreCase = true) ||
            (normalizedDigits.length == 10 && AuthSecurityUtil.extractNormalized10Digits(user.mobileNumber) == normalizedDigits)
        }
    }

    /**
     * Checks if at least one user account exists locally.
     */
    fun hasAnyUser(): Boolean {
        return getUsers().isNotEmpty()
    }

    /**
     * Saves login session state without modifying stored accounts or transaction logs.
     */
    fun setLoggedIn(loggedIn: Boolean, userEmail: String? = null) {
        val editor = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn)
        if (loggedIn && userEmail != null) {
            editor.putString(KEY_ACTIVE_USER_EMAIL, userEmail)
        } else if (!loggedIn) {
            editor.remove(KEY_ACTIVE_USER_EMAIL)
        }
        editor.apply()
    }

    /**
     * Checks whether an authenticated session is currently active.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Retrieves the currently logged-in user account, if any.
     */
    fun getActiveUser(): UserAccount? {
        if (!isLoggedIn()) return null
        val activeEmail = prefs.getString(KEY_ACTIVE_USER_EMAIL, null) ?: return null
        return getUsers().firstOrNull { it.email.equals(activeEmail, ignoreCase = true) }
    }

    /**
     * Purges registered users (used for testing or clean resets).
     */
    fun clearUsers() {
        prefs.edit()
            .remove(KEY_USERS)
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_ACTIVE_USER_EMAIL)
            .apply()
    }
}
