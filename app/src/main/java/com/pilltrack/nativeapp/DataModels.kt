package com.pilltrack.nativeapp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate

data class PillLog(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val dose: String,
    val time: Long,
    val color: String,
    val stomach: String = "empty",
    val parsedDose: Float = 0f,
    val halfLife: Float = 3.0f,
    val foodFactor: Float = 0.4f
)

data class UserProfile(
    val nickname: String = "默认用户",
    val signature: String = "坚持记录，关注健康",
    val avatarPath: String? = null,
    val backgroundPath: String? = null,
    val backgroundAlpha: Float = 1.0f,
    val backgroundScale: Float = 1.0f,
    val backgroundRotation: Float = 0f,
    val backgroundOffsetX: Float = 0f,
    val backgroundOffsetY: Float = 0f,
    val themeMode: String = "system"
)

data class InventoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val dose: String,
    val stomach: String = "empty",
    val quantity: Int,
    val totalCapacity: Int
)

object LocalStorage {
    private const val PREFS_NAME = "pilltrack_prefs"
    private const val KEY_LOGS = "pilltrack_logs"
    private const val KEY_PROFILE = "pilltrack_profile"
    private const val KEY_CUSTOM_DRUGS = "pilltrack_custom_drugs"
    private const val KEY_INVENTORY = "pilltrack_inventory"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLogs(context: Context, logs: List<PillLog>) {
        try {
            val json = Gson().toJson(logs)
            val encryptedJson = CryptoManager.encrypt(json.toByteArray())
            val dst = File(context.filesDir, "pill_logs.enc")
            val tmp = File(context.filesDir, "pill_logs.enc.tmp")
            val bak = File(context.filesDir, "pill_logs.enc.bak")
            tmp.writeText(encryptedJson)
            if (dst.exists()) dst.copyTo(bak, overwrite = true)
            tmp.renameTo(dst)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Save encrypted to prefs
        try {
            val encryptedJson = CryptoManager.encrypt(Gson().toJson(logs).toByteArray())
            getPrefs(context).edit().putString(KEY_LOGS, encryptedJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLogs(context: Context): List<PillLog> {
        val type = object : TypeToken<List<PillLog>>() {}.type
        fun parseEncrypted(encryptedText: String?): List<PillLog>? = try {
            if (encryptedText.isNullOrEmpty()) null
            else {
                val decrypted = String(CryptoManager.decrypt(encryptedText))
                Gson().fromJson<List<PillLog>>(decrypted, type)
            }
        } catch (e: Exception) {
            null
        }

        try {
            val dst = File(context.filesDir, "pill_logs.enc")
            if (dst.exists()) parseEncrypted(dst.readText())?.let { return it }
            val bak = File(context.filesDir, "pill_logs.enc.bak")
            if (bak.exists()) parseEncrypted(bak.readText())?.let { return it }

            // Migration block for old plaintext json file
            val oldPlaintextDst = File(context.filesDir, "pill_logs.json")
            if (oldPlaintextDst.exists()) {
                val plaintext = oldPlaintextDst.readText()
                val oldLogs = Gson().fromJson<List<PillLog>>(plaintext, type)
                if (oldLogs != null) {
                    saveLogs(context, oldLogs) // Resave encrypted
                    oldPlaintextDst.delete()
                    File(context.filesDir, "pill_logs.json.bak").delete()
                    return oldLogs
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val encryptedPrefsStr = getPrefs(context).getString(KEY_LOGS, null) ?: return emptyList()
        return parseEncrypted(encryptedPrefsStr) ?: emptyList()
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val gson = Gson()
        val json = gson.toJson(profile)
        try {
            val encryptedJson = CryptoManager.encrypt(json.toByteArray())
            getPrefs(context).edit().putString(KEY_PROFILE, encryptedJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadProfile(context: Context): UserProfile {
        val encryptedJson = getPrefs(context).getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            val decryptedJson = String(CryptoManager.decrypt(encryptedJson))
            Gson().fromJson(decryptedJson, UserProfile::class.java) ?: UserProfile()
        } catch (e: Exception) {
            try {
                // Migration fallback for legacy plaintext profiles
                val legacyProfile = Gson().fromJson(encryptedJson, UserProfile::class.java)
                if (legacyProfile != null) {
                    saveProfile(context, legacyProfile) // Auto-migrate to encrypted storage
                    return legacyProfile
                }
            } catch (innerE: Exception) {
                // Ignore
            }
            UserProfile()
        }
    }

    fun saveCustomDrugs(context: Context, drugs: List<Triple<String, String, String>>) {
        try {
            val json = Gson().toJson(drugs)
            val encryptedJson = CryptoManager.encrypt(json.toByteArray())
            getPrefs(context).edit().putString(KEY_CUSTOM_DRUGS, encryptedJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomDrugs(context: Context): List<Triple<String, String, String>> {
        val defaultList = listOf(
            Triple("布洛芬", "400", "empty"),
            Triple("对乙酰氨基酚", "500", "full"),
            Triple("阿莫西林", "500", "full"),
            Triple("维生素C", "100", "full"),
            Triple("氯雷他定", "10", "empty"),
            Triple("阿司匹林", "100", "full")
        )
        val encryptedJson = getPrefs(context).getString(KEY_CUSTOM_DRUGS, null)
        if (encryptedJson.isNullOrEmpty()) {
            return defaultList
        }
        return try {
            val decryptedJson = String(CryptoManager.decrypt(encryptedJson))
            val type = object : TypeToken<List<Triple<String, String, String>>>() {}.type
            Gson().fromJson(decryptedJson, type) ?: defaultList
        } catch (e: Exception) {
            try {
                val type = object : TypeToken<List<Triple<String, String, String>>>() {}.type
                val legacyList = Gson().fromJson<List<Triple<String, String, String>>>(encryptedJson, type)
                if (legacyList != null) {
                    saveCustomDrugs(context, legacyList)
                    return legacyList
                }
            } catch (inner: Exception) { }
            defaultList
        }
    }

    fun saveInventory(context: Context, inventory: List<InventoryItem>) {
        try {
            val json = Gson().toJson(inventory)
            val encryptedJson = CryptoManager.encrypt(json.toByteArray())
            getPrefs(context).edit().putString(KEY_INVENTORY, encryptedJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadInventory(context: Context): List<InventoryItem> {
        val encryptedJson = getPrefs(context).getString(KEY_INVENTORY, null) ?: return emptyList()
        return try {
            val decryptedJson = String(CryptoManager.decrypt(encryptedJson))
            val type = object : TypeToken<List<InventoryItem>>() {}.type
            Gson().fromJson(decryptedJson, type) ?: emptyList()
        } catch (e: Exception) {
            try {
                val type = object : TypeToken<List<InventoryItem>>() {}.type
                val legacyList = Gson().fromJson<List<InventoryItem>>(encryptedJson, type)
                if (legacyList != null) {
                    saveInventory(context, legacyList)
                    return legacyList
                }
            } catch (inner: Exception) { }
            emptyList()
        }
    }

    fun deductInventory(context: Context, medName: String, dose: String) {
        val inventory = loadInventory(context).toMutableList()
        val itemIndex = inventory.indexOfFirst { it.name == medName && it.dose == dose }
        if (itemIndex != -1) {
            val item = inventory[itemIndex]
            if (item.quantity > 0) {
                inventory[itemIndex] = item.copy(quantity = item.quantity - 1)
                saveInventory(context, inventory)
            }
        }
    }

    fun saveAvatarImage(context: Context, inputStream: InputStream): String {
        val avatarFile = File(context.filesDir, "user_avatar.jpg")
        FileOutputStream(avatarFile).use { out ->
            inputStream.copyTo(out)
        }
        return avatarFile.absolutePath
    }

    // P1-2: 采样解码，4K壁纸/头像压到reqSize以内再进内存，防OOM
    fun decodeSampledFile(path: String?, reqSize: Int = 1080): Bitmap? {
        if (path == null) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > reqSize || bounds.outHeight / sample > reqSize) sample *= 2
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    // Use ARGB_8888 for better quality (especially for avatars) instead of RGB_565
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledBytes(bytes: ByteArray, reqSize: Int = 1920): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > reqSize || bounds.outHeight / sample > reqSize) sample *= 2
            BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun loadAvatarBitmap(path: String?): Bitmap? {
        return decodeSampledFile(path, 1080)
    }

    fun saveBackgroundImage(context: Context, inputStream: InputStream): String {
        val bgFile = File(context.filesDir, "custom_background.jpg")
        FileOutputStream(bgFile).use { out ->
            inputStream.copyTo(out)
        }
        return bgFile.absolutePath
    }

    fun loadBackgroundBitmap(path: String?): Bitmap? {
        return decodeSampledFile(path, 1920)
    }

    fun deleteBackgroundImage(context: Context) {
        val bgFile = File(context.filesDir, "custom_background.jpg")
        if (bgFile.exists()) {
            bgFile.delete()
        }
    }

    // P1-4: java.time替代SimpleDateFormat/Calendar，线程安全+跨天不错
    private fun logLocalDate(timeMillis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

    private val weekNames = arrayOf("一", "二", "三", "四", "五", "六", "日")

    // Check past 7 days (index 0 is 6 days ago, index 6 is today)
    fun getMonthAdherence(logs: List<PillLog>): List<Pair<Int, Boolean>> {
        val logDates = logs.map { logLocalDate(it.time) }.toSet()

        val today = LocalDate.now()
        val daysInMonth = today.lengthOfMonth()
        return (1..daysInMonth).map { day ->
            val d = today.withDayOfMonth(day)
            Pair(day, logDates.contains(d))
        }
    }
}
