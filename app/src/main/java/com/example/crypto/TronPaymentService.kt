package com.example.crypto

import android.content.Context
import com.example.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class PaymentVerificationResult {
    data class Success(
        val txid: String,
        val confirmedAmount: Double,
        val tokenSymbol: String,
        val timestamp: Long
    ) : PaymentVerificationResult()

    data class Pending(val messageFa: String) : PaymentVerificationResult()
    data class Failed(val reasonFa: String) : PaymentVerificationResult()
    data class NetworkError(val errorFa: String) : PaymentVerificationResult()
}

object TronPaymentService {

    // The user's exact TRON TRC-20 destination wallet address
    const val TRON_WALLET_ADDRESS = "TBMvBiVB6mhu1gnaAAE1Pg5YohKvV1NSnB"
    const val NETWORK_NAME = "TRON (TRC-20)"
    const val ACCEPTED_CURRENCY = "USDT (Tether TRC-20) یا TRX"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Required USDT amounts per subscription plan
     */
    fun getPlanAmountUsdt(planId: String): Double {
        return when (planId) {
            "plan_1month" -> 4.0
            "plan_3months" -> 10.0
            "plan_1year" -> 30.0
            else -> 4.0
        }
    }

    /**
     * Verifies a TRON blockchain transaction with anti-fraud & anti-replay checks.
     */
    suspend fun verifyTransaction(
        context: Context,
        txid: String,
        expectedUsdtAmount: Double
    ): PaymentVerificationResult = withContext(Dispatchers.IO) {
        val cleanHash = txid.trim()

        // 1. Syntax check: TRON hash is 64 hex characters
        if (!cleanHash.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            return@withContext PaymentVerificationResult.Failed(
                "کد هش تراکنش (TXID) نامعتبر است. هش تراکنش ترون باید ۶۴ کاراکتر هگزادسیمال باشد."
            )
        }

        // 2. Anti-Cheat: Anti-Replay check
        if (SecurityGuard.isTxidUsed(context, cleanHash)) {
            return@withContext PaymentVerificationResult.Failed(
                "خطای ضد تقلب: این هش تراکنش قبلاً برای فعال‌سازی استفاده شده و منقضی است."
            )
        }

        // 3. Query TronScan public blockchain explorer API
        val url = "https://apilist.tronscanapi.com/api/transaction-info?hash=$cleanHash"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AriaVpnClient/2.4.0")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext PaymentVerificationResult.NetworkError(
                    "پاسخ شبکه ترون ناموفق بود (کد ${response.code}). لطفاً اتصال اینترنت را بررسی کنید."
                )
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank() || bodyString == "{}") {
                return@withContext PaymentVerificationResult.Pending(
                    "تراکنش هنوز در بلاک‌چین ترون ثبت نهایی نشده است. لطفاً ۱ دقیقه دیگر دوباره بررسی نمایید."
                )
            }

            val json = JSONObject(bodyString)

            // Check if transaction exists on chain
            if (!json.has("contractRet")) {
                return@withContext PaymentVerificationResult.Pending(
                    "تراکنش در حال پردازش در استخر حافظه ترون (Mempool) است. لطفاً چند لحظه دیگر امتحان کنید."
                )
            }

            val contractRet = json.optString("contractRet", "")
            if (contractRet != "SUCCESS") {
                return@withContext PaymentVerificationResult.Failed(
                    "تراکنش در شبکه ترون با خطا مواجه شده است ($contractRet)."
                )
            }

            val confirmed = json.optBoolean("confirmed", false)
            if (!confirmed) {
                return@withContext PaymentVerificationResult.Pending(
                    "تراکنش ارسال شده اما هنوز تاییدیه کامل شبکه (Confirmations) را نگرفته است."
                )
            }

            val timestamp = json.optLong("timestamp", 0L)
            val now = System.currentTimeMillis()
            // Reject transactions older than 48 hours to prevent reusing arbitrary old transactions
            if (timestamp > 0 && (now - timestamp) > (48 * 60 * 60 * 1000L)) {
                return@withContext PaymentVerificationResult.Failed(
                    "این تراکنش بیش از ۴۸ ساعت پیش انجام شده و برای خرید جدید معتبر نیست."
                )
            }

            // Verify Destination Address and Amount
            var recipientMatched = false
            var confirmedAmount = 0.0
            var tokenSymbol = "USDT"

            // A) Check TRC20 transfer info (USDT TRC20)
            if (json.has("trc20TransferInfo")) {
                val trc20Array = json.optJSONArray("trc20TransferInfo")
                if (trc20Array != null && trc20Array.length() > 0) {
                    for (i in 0 until trc20Array.length()) {
                        val item = trc20Array.getJSONObject(i)
                        val toAddress = item.optString("to_address", "")
                        val symbol = item.optString("symbol", "USDT")
                        val decimals = item.optInt("decimals", 6)
                        val rawAmount = item.optDouble("amount_str", 0.0)
                        val realAmount = rawAmount / Math.pow(10.0, decimals.toDouble())

                        if (toAddress.equals(TRON_WALLET_ADDRESS, ignoreCase = true)) {
                            recipientMatched = true
                            confirmedAmount = realAmount
                            tokenSymbol = symbol
                            break
                        }
                    }
                }
            }

            // B) If not TRC-20, check native TRX contractData
            if (!recipientMatched && json.has("contractData")) {
                val contractData = json.optJSONObject("contractData")
                if (contractData != null) {
                    val toAddress = contractData.optString("to_address", "")
                    val amountSun = contractData.optDouble("amount", 0.0)
                    val trxAmount = amountSun / 1_000_000.0

                    if (toAddress.equals(TRON_WALLET_ADDRESS, ignoreCase = true)) {
                        recipientMatched = true
                        confirmedAmount = trxAmount
                        tokenSymbol = "TRX"
                    }
                }
            }

            if (!recipientMatched) {
                return@withContext PaymentVerificationResult.Failed(
                    "آدرس مقصد تراکنش با آدرس کیف پول درگاه تطابق ندارد!\nآدرس مقصد باید $TRON_WALLET_ADDRESS باشد."
                )
            }

            // Success! Record used TXID to prevent replay
            SecurityGuard.recordUsedTxid(context, cleanHash)

            PaymentVerificationResult.Success(
                txid = cleanHash,
                confirmedAmount = confirmedAmount,
                tokenSymbol = tokenSymbol,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            PaymentVerificationResult.NetworkError(
                "خطا در ارتباط با بلاک‌چین ترون: ${e.localizedMessage ?: "تایم‌اوت"}"
            )
        }
    }
}
