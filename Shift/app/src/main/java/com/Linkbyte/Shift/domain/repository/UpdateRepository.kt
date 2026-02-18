package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.AppUpdateInfo

interface UpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdateInfo?>
    suspend fun getLatestVersionInfo(): Result<AppUpdateInfo>
}
