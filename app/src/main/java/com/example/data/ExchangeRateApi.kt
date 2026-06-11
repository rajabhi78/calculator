package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @Json(name = "result") val result: String,
    @Json(name = "base_code") val baseCode: String,
    @Json(name = "rates") val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateResponse

    companion object {
        private const val BASE_URL = "https://open.er-api.com/"

        // Default offline backup rates dictionary (relative to USD)
        val DEFAULT_RATES = mapOf(
            "USD" to 1.0,
            "INR" to 83.56,
            "EUR" to 0.92,
            "GBP" to 0.78,
            "JPY" to 157.12,
            "AED" to 3.67,
            "CAD" to 1.37,
            "AUD" to 1.51,
            "CHF" to 0.89,
            "CNY" to 7.25,
            "HKD" to 7.81,
            "SGD" to 1.35,
            "SAR" to 3.75,
            "KRW" to 1378.0,
            "NZD" to 1.63,
            "MXN" to 18.25,
            "BRL" to 5.36,
            "ZAR" to 18.84,
            "RUB" to 89.15,
            "TRY" to 32.40,
            "SEK" to 10.45,
            "NOK" to 10.65,
            "DKK" to 6.88,
            "IDR" to 16250.0,
            "MYR" to 4.71,
            "PHP" to 58.60,
            "THB" to 36.75,
            "VND" to 25450.0,
            "ILS" to 3.72,
            "PLN" to 4.02,
            "KWD" to 0.31,
            "QAR" to 3.64,
            "OMR" to 0.38,
            "BHD" to 0.38,
            "EGP" to 47.60
        )

        // Metadata defining flag and name symbols of supported currencies
        val CURRENCY_METADATA = mapOf(
            "INR" to Pair("Indian Rupee", "🇮🇳"),
            "USD" to Pair("US Dollar", "🇺🇸"),
            "EUR" to Pair("Euro", "🇪🇺"),
            "GBP" to Pair("British Pound", "🇬🇧"),
            "JPY" to Pair("Japanese Yen", "🇯🇵"),
            "AED" to Pair("UAE Dirham", "🇦🇪"),
            "CAD" to Pair("Canadian Dollar", "🇨🇦"),
            "AUD" to Pair("Australian Dollar", "🇦🇺"),
            "CHF" to Pair("Swiss Franc", "🇨🇭"),
            "CNY" to Pair("Chinese Yuan", "🇨🇳"),
            "HKD" to Pair("Hong Kong Dollar", "🇭🇰"),
            "SGD" to Pair("Singapore Dollar", "🇸🇬"),
            "SAR" to Pair("Saudi Riyal", "🇸🇦"),
            "KRW" to Pair("South Korean Won", "🇰🇷"),
            "NZD" to Pair("New Zealand Dollar", "🇳🇿"),
            "MXN" to Pair("Mexican Peso", "🇲🇽"),
            "BRL" to Pair("Brazilian Real", "🇧🇷"),
            "ZAR" to Pair("South African Rand", "🇿🇦"),
            "RUB" to Pair("Russian Ruble", "🇷🇺"),
            "TRY" to Pair("Turkish Lira", "🇹🇷"),
            "SEK" to Pair("Swedish Krona", "🇸🇪"),
            "NOK" to Pair("Norwegian Krone", "🇳🇴"),
            "DKK" to Pair("Danish Krone", "🇩🇰"),
            "IDR" to Pair("Indonesian Rupiah", "🇮🇩"),
            "MYR" to Pair("Malaysian Ringgit", "🇲🇾"),
            "PHP" to Pair("Philippine Peso", "🇵🇭"),
            "THB" to Pair("Thai Baht", "🇹🇭"),
            "VND" to Pair("Vietnamese Dong", "🇻🇳"),
            "ILS" to Pair("Israeli Shekel", "🇮🇱"),
            "PLN" to Pair("Polish Zloty", "🇵🇱"),
            "KWD" to Pair("Kuwaiti Dinar", "🇰🇼"),
            "QAR" to Pair("Qatari Riyal", "🇶🇦"),
            "OMR" to Pair("Omani Rial", "🇴🇲"),
            "BHD" to Pair("Bahraini Dinar", "🇧🇭"),
            "EGP" to Pair("Egyptian Pound", "🇪🇬")
        )

        val instance: ExchangeRateApi by lazy {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ExchangeRateApi::class.java)
        }
    }
}
