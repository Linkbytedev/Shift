package com.Linkbyte.Shift.data.repository

import android.util.Log
import com.Linkbyte.Shift.BuildConfig
import com.Linkbyte.Shift.data.model.AppUpdateInfo
import com.Linkbyte.Shift.domain.repository.UpdateRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : UpdateRepository {

    private val UPDATE_URL = "https://linkbytedev.github.io/Linkbytesite/version.json" 

    private suspend fun fetchRemoteConfig(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_URL)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch update info: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)

            val info = AppUpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.getString("versionName"),
                downloadUrl = json.getString("downloadUrl"),
                releaseNotes = json.optString("releaseNotes", "")
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.e("UpdateRepository", "Error fetching remote config", e)
            Result.failure(e)
        }
    }

    override suspend fun getLatestVersionInfo(): Result<AppUpdateInfo> = fetchRemoteConfig()

    override suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        val remoteResult = fetchRemoteConfig()
        if (remoteResult.isFailure) {
            return@withContext Result.failure(remoteResult.exceptionOrNull()!!)
        }

        val remoteInfo = remoteResult.getOrNull()!!
        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE

        val isUpdate = if (remoteInfo.versionCode > currentVersionCode) {
            true
        } else if (remoteInfo.versionCode < currentVersionCode && remoteInfo.versionCode != 0) {
            false
        } else {
            isUpdateAvailable(currentVersionName, remoteInfo.versionName)
        }

        if (isUpdate) {
            Result.success(remoteInfo)
        } else {
            Result.success(null) // No update needed
        }
    }

    /**
     * Compares two version strings (e.g., "1.0.0" vs "1.0.1").
     * Returns true if remoteVersion > currentVersion.
     */
    private fun isUpdateAvailable(current: String, remote: String): Boolean {
        try {
            val currentNormalized = current.lowercase()
            val remoteNormalized = remote.lowercase()

            val currentParts = currentNormalized.split(".")
            val remoteParts = remoteNormalized.split(".")

            val length = maxOf(currentParts.size, remoteParts.size)

            for (i in 0 until length) {
                val cStr = currentParts.getOrElse(i) { "0" }
                val rStr = remoteParts.getOrElse(i) { "0" }

                val cNum = cStr.filter { it.isDigit() }.toIntOrNull() ?: 0
                val rNum = rStr.filter { it.isDigit() }.toIntOrNull() ?: 0

                if (rNum > cNum) return true
                if (rNum < cNum) return false
                
                // If numbers are equal, check for "beta" label
                // A version WITHOUT "beta" is newer than a version WITH "beta"
                val cHasBeta = cStr.contains("beta")
                val rHasBeta = rStr.contains("beta")
                
                if (cHasBeta && !rHasBeta) return true // Current is beta, remote is stable -> Update
                if (!cHasBeta && rHasBeta) return false // Current is stable, remote is beta -> No update
            }
        } catch (e: Exception) {
            Log.e("UpdateRepository", "Error comparing versions: $current vs $remote", e)
        }
        return false
    }
}
