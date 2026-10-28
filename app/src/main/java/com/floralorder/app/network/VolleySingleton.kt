package com.floralorder.app.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * The class name is kept so the Activities don't need to change, but there is
 * no Volley or internet involved anymore: requests are answered locally.
 */
class VolleySingleton private constructor(context: Context) {
    val requestQueue = LocalRequestQueue(context.applicationContext)

    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null

        fun getInstance(context: Context): VolleySingleton =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also { INSTANCE = it }
            }
    }
}

class LocalRequestQueue(private val context: Context) {
    // One background thread keeps database work off the UI thread and in order.
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun add(request: JsonObjectRequest) {
        executor.execute {
            try {
                val result = LocalApi.handle(context, request.url, request.jsonRequest)
                mainHandler.post { request.listener(result) }
            } catch (e: Exception) {
                mainHandler.post { request.errorListener(e) }
            }
        }
    }
}
