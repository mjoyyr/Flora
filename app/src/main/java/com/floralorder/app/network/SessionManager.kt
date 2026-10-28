package com.floralorder.app.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("floral_session", Context.MODE_PRIVATE)

    fun saveUser(
        userId: Int,
        name: String,
        email: String,
        role: String = "customer",
        phone: String = "",
        address: String = ""
    ) {
        prefs.edit()
            .putInt("user_id", userId)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_role", role)
            .putString("user_phone", phone)
            .putString("user_address", address)
            .apply()
    }

    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getUserEmail(): String = prefs.getString("user_email", "") ?: ""
    fun getUserRole(): String = prefs.getString("user_role", "customer") ?: "customer"
    fun getUserPhone(): String = prefs.getString("user_phone", "") ?: ""
    fun getUserAddress(): String = prefs.getString("user_address", "") ?: ""
    fun isLoggedIn(): Boolean = getUserId() != -1
    fun isAdmin(): Boolean = getUserRole() == "admin"

    fun logout() {
        prefs.edit().clear().apply()
    }
}
