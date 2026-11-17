
package com.teddy.bpositive.data

import okhttp3.OkHttpClient
import okhttp3.Request
import android.os.Handler
import android.os.Looper

object SupabaseClient {

    private val client = OkHttpClient()

    fun testConnectionAsync(onResult: (String) -> Unit) {

        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/auth/v1/settings")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader(
                    "Authorization",
                    "Bearer ${SupabaseConfig.API_KEY}"
                )
                .get()
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        "Connected successfully! HTTP ${response.code}"
                    } else {
                        "Connection failed: HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }

            Handler(Looper.getMainLooper()).post {
                onResult(result)
            }

        }.start()
    }
}
