package com.phamnhantucode.aicareercoach.data.pdf

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class APITemplateService {

    companion object {
        private const val API_URL = "https://rest.apitemplate.io/v2/create-pdf-from-html"
        private const val AUTH_KEY = "c0f0NDM2NDA6NDA4NDg6V042YmJ4RU5zZDJKSnRTZQ="
        private const val TAG = "APITemplateService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class PdfRequest(
        @SerializedName("html") val html: String,
        @SerializedName("css") val css: String? = null,
        @SerializedName("settings") val settings: PdfSettings? = null
    )

    data class PdfSettings(
        @SerializedName("paper_size") val paperSize: String = "A4",
        @SerializedName("orientation") val orientation: String = "1", // 1=Portrait, 2=Landscape
        @SerializedName("header_font_size") val headerFontSize: String = "9px",
        @SerializedName("margin_top") val marginTop: String = "30",
        @SerializedName("margin_right") val marginRight: String = "30",
        @SerializedName("margin_bottom") val marginBottom: String = "30",
        @SerializedName("margin_left") val marginLeft: String = "30",
        @SerializedName("footer_font_size") val footerFontSize: String = "9px"
    )

    fun generatePdf(html: String, css: String = ""): ByteArray {
        val requestBody = PdfRequest(
            html = html,
            css = css,
            settings = PdfSettings()
        )

        val jsonBody = gson.toJson(requestBody)
        
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("X-API-KEY", AUTH_KEY)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "PDF generation failed: ${response.code} - $errorBody")
                throw IOException("PDF generation failed: ${response.code} ${response.message}")
            }

            return response.body?.bytes() ?: throw IOException("Empty response from PDF service")
        }
    }
}
