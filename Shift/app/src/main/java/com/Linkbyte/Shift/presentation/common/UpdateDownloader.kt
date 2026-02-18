package com.Linkbyte.Shift.presentation.common

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.Linkbyte.Shift.BuildConfig
import java.io.File

object UpdateDownloader {

    private var downloadId: Long = -1

    fun downloadApk(context: Context, url: String) {
        // Delete existing file if it exists to prevent renaming (e.g., ShiftUpdate-1.apk)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ShiftUpdate.apk")
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Update")
            .setDescription("Downloading latest version of Shift...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ShiftUpdate.apk")
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()

        // Register receiver for download complete
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    // Check if download was successful
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val uriString = cursor.getString(uriIndex)
                            if (uriString != null) {
                                installApk(ctxt, Uri.parse(uriString))
                            }
                        }
                    }
                    cursor.close()

                    try {
                        ctxt.unregisterReceiver(this)
                    } catch (e: IllegalArgumentException) {
                        // Receiver might not be registered or already unregistered
                    }
                }
            }
        }
        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(context: Context, downloadUri: Uri) {
        try {
            val path = downloadUri.path ?: return
            
            // If the URI is a content URI (which it shouldn't be for setDestinationInExternalFilesDir, but safest to check),
            // we might need to handle it differently. However, typically it returns a file:// URI for external files.
            // Let's rely on constructing the file from the known path if possible, or use the URI directly if it's a content URI.

            // Reliable way: Get the file path we requested. Even if DownloadManager renamed it, 
            // we cleared it first, so it SHOULD be ShiftUpdate.apk. 
            // BUT, to be absolutely safe, let's use the file we explicitly cleared and requested.
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ShiftUpdate.apk")

            if (targetFile.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    targetFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                context.startActivity(intent)
            } else {
                 Toast.makeText(context, "Update file not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error installing update: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
