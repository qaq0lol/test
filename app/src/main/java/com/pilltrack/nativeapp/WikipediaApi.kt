package com.pilltrack.nativeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MedInfo(
    val medication: String,
    val halfLifeHours: Float,
    val standardDose: Float,
    val unit: String,
    val foodEffectKaMultiplier: Float
)

object WikipediaApi {
    // P1-3: 内存缓存 + 本地常用药兜底，断网/解析失败不画错曲线
    private val cache = mutableMapOf<String, MedInfo>()

    private fun localFallback(medName: String): MedInfo {
        val lower = medName.lowercase()
        var hl = 3.0f
        var stdDose = 100f
        val unit = "mg"
        when {
            lower.contains("布洛芬") || lower.contains("ibuprofen") -> { hl = 2.0f; stdDose = 400f }
            lower.contains("对乙酰氨基酚") || lower.contains("扑热息痛") || lower.contains("paracetamol") -> { hl = 2.5f; stdDose = 500f }
            lower.contains("阿司匹林") || lower.contains("aspirin") -> { hl = 0.25f; stdDose = 100f }
            lower.contains("阿莫西林") || lower.contains("amoxicillin") -> { hl = 1.0f; stdDose = 500f }
            lower.contains("氯雷他定") || lower.contains("loratadine") -> { hl = 8.0f; stdDose = 10f }
            lower.contains("西替利嗪") || lower.contains("cetirizine") -> { hl = 8.3f; stdDose = 10f }
            lower.contains("维生素c") || lower.contains("vc") -> { hl = 2.0f; stdDose = 100f }
        }
        return MedInfo(medName, hl, stdDose, unit, 0.4f)
    }

    suspend fun searchMedicationLocal(medName: String): MedInfo = withContext(Dispatchers.IO) {
        cache[medName]?.let { return@withContext it }
        var hl: Float? = null
        var fullFactor = 0.4f

        var connection: HttpURLConnection? = null
        try {
            val encodedName = URLEncoder.encode(medName, "UTF-8")
            val urlString = "https://zh.wikipedia.org/w/api.php?action=query&prop=extracts&exsentences=20&explaintext=1&titles=$encodedName&format=json&origin=*"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            // P1-3: 超时，弱网不等死
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val query = jsonObject.optJSONObject("query")
                val pages = query?.optJSONObject("pages")
                
                pages?.keys()?.forEach { pageId ->
                    val page = pages.optJSONObject(pageId)
                    val extract = page?.optString("extract") ?: ""
                    if (extract.isNotEmpty()) {
                        val match = Regex("半衰期.*?(\\d+(?:\\.\\d+)?)\\s*(小?时|分钟)").find(extract)
                        if (match != null) {
                            var hlValue = match.groupValues[1].toFloat()
                            if (match.groupValues[2].contains("分") && !match.groupValues[2].contains("时")) {
                                hlValue /= 60f
                            }
                            hl = hlValue
                        } else {
                            val matchRange = Regex("半衰期.*?(\\d+(?:\\.\\d+)?(?:-\\d+(?:\\.\\d+)?)?)\\s*个?小时").find(extract)
                            if (matchRange != null) {
                                val numStr = matchRange.groupValues[1]
                                if (numStr.contains("-")) {
                                    val parts = numStr.split("-")
                                    hl = (parts[0].toFloat() + parts[1].toFloat()) / 2.0f
                                } else {
                                    hl = numStr.toFloat()
                                }
                            }
                        }

                        if (Regex("餐后|饱腹|进食|食物").containsMatchIn(extract)) {
                            if (Regex("延迟|减慢|下降|减少").containsMatchIn(extract)) fullFactor = 0.4f
                            else if (Regex("促进|加快|增加|提高").containsMatchIn(extract)) fullFactor = 1.5f
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        // 联网没解析出半衰期就用本地表，不让曲线乱画
        if (hl == null) {
            val fallback = localFallback(medName)
            // 保留本次解析出的foodFactor（默认0.4）
            val result = fallback.copy(foodEffectKaMultiplier = fullFactor)
            cache[medName] = result
            return@withContext result
        }

        val lower = medName.lowercase()
        var stdDose = 100f
        var unit = "mg"

        when {
            lower.contains("布洛芬") || lower.contains("ibuprofen") -> { hl = hl ?: 2.0f; stdDose = 400f }
            lower.contains("对乙酰氨基酚") || lower.contains("扑热息痛") || lower.contains("paracetamol") -> { hl = hl ?: 2.5f; stdDose = 500f }
            lower.contains("阿司匹林") || lower.contains("aspirin") -> { hl = hl ?: 0.25f; stdDose = 100f }
            lower.contains("阿莫西林") || lower.contains("amoxicillin") -> { hl = hl ?: 1.0f; stdDose = 500f }
            lower.contains("氯雷他定") || lower.contains("loratadine") -> { hl = hl ?: 8.0f; stdDose = 10f }
            lower.contains("西替利嗪") || lower.contains("cetirizine") -> { hl = hl ?: 8.3f; stdDose = 10f }
            lower.contains("维生素c") || lower.contains("vc") -> { hl = hl ?: 2.0f; stdDose = 100f }
            else -> { hl = hl ?: 3.0f }
        }

        val result = MedInfo(medName, hl!!, stdDose, unit, fullFactor)
        cache[medName] = result
        result
    }
}
