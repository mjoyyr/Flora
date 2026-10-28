package com.floralorder.app.network

import org.json.JSONObject

/**
 * Drop-in replacements for Volley's Request.Method and JsonObjectRequest.
 * The Activities keep their existing code, but the request is now answered by
 * the SQLite database on the phone (see LocalApi) instead of a PHP server.
 */
object Request {
    object Method {
        const val GET = 0
        const val POST = 1
    }
}

class JsonObjectRequest(
    val method: Int,
    val url: String,
    val jsonRequest: JSONObject?,
    val listener: (JSONObject) -> Unit,
    val errorListener: (Exception) -> Unit
)
