package com.nll.store

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.nll.store.installer.FileDownloader
import com.nll.store.log.CLog
import java.io.File

class ApkAttachmentProvider : ContentProvider() {
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    companion object {
        private const val logTag = "ApkAttachmentProvider"

        private const val shareMatchPath = "share"
        private const val shareMatchId = 1

        //Important that it is matched to Manifest
        private fun buildRecordingFileShareAuthority(context: Context) = "${context.packageName}.ApkAttachmentProvider"

        private fun getContentUri(context: Context) = Uri.parse("content://${buildRecordingFileShareAuthority(context)}").buildUpon().appendPath(shareMatchPath).build()

        fun getUri(context: Context, fileName: String): Uri = getContentUri(context).buildUpon().appendPath(fileName).build()


    }


    override fun onCreate(): Boolean {
        if (CLog.isDebug()) {
            CLog.log(logTag, "onCreate()")
        }
        uriMatcher.addURI(buildRecordingFileShareAuthority(requireNotNull(context)), "$shareMatchPath/*", shareMatchId)
        return true
    }


    override fun query(incomingUri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (CLog.isDebug()) {
            CLog.log(logTag, "query() -> uri: $incomingUri")
            CLog.log(logTag, "query() -> file : ${incomingUri.lastPathSegment}")
        }

        if (Uri.decode(incomingUri.toString()).contains("../")) {
            throw SecurityException("$incomingUri is not allowed")
        }


        val fileNameFromUri = incomingUri.lastPathSegment
        if (fileNameFromUri.isNullOrEmpty()) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "query() -> file is NULL!")
            }
            return null
        }

        if (uriMatcher.match(incomingUri) != shareMatchId) {

            if (CLog.isDebug()) {
                CLog.log(logTag, "query() -> uriMatcher cannot match to $shareMatchId")
            }
        }
        if (context == null) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "query() -> context is NULL")
            }
            return null
        }
        val fileToSend = File(FileDownloader.getBaseFolder(requireNotNull(context)), fileNameFromUri)
        if (!fileToSend.exists()) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "query() -> Cannot find $fileNameFromUri")
            }
            return null
        }

        //In case projection is null. Some apps like Telegram or TotalCommander does that
        val localProjection = projection
            ?: arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
        val matrixCursor = MatrixCursor(localProjection)
        val rowBuilder = matrixCursor.newRow()
        matrixCursor.columnNames.forEach { column ->
            when {

                column.equals(MediaStore.MediaColumns.DISPLAY_NAME, ignoreCase = true) -> {
                    rowBuilder.add(column, fileNameFromUri)
                }

                column.equals(MediaStore.MediaColumns.SIZE, ignoreCase = true) -> {
                    rowBuilder.add(column, fileToSend.length())
                }

                column.equals(MediaStore.MediaColumns.MIME_TYPE, ignoreCase = true) -> {
                    rowBuilder.add(column, "application/vnd.android.package-archive")
                }

                column.equals(MediaStore.MediaColumns.DATE_MODIFIED, ignoreCase = true) ||
                        column.equals(MediaStore.MediaColumns.DATE_ADDED, ignoreCase = true) -> {
                    rowBuilder.add(column, fileToSend.lastModified())
                }
            }
        }
        return matrixCursor
    }


    override fun openFile(incomingUri: Uri, mode: String): ParcelFileDescriptor? {
        if (CLog.isDebug()) {
            CLog.log(logTag, "openFile() ->  incomingUri: $incomingUri")
        }
        if (Uri.decode(incomingUri.toString()).contains("../")) {
            throw SecurityException("$incomingUri is not allowed")
        }


        val fileNameFromUri = incomingUri.lastPathSegment
        if (fileNameFromUri.isNullOrEmpty()) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "openFile() -> file name is NULL!")
            }
            return null
        }

        if (uriMatcher.match(incomingUri) != shareMatchId) {

            if (CLog.isDebug()) {
                CLog.log(logTag, "openFile() -> uriMatcher cannot match to $shareMatchId")
            }
        }
        if (context == null) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "openFile() -> context is NULL")
            }
            return null
        }

        val fileToSend = File(FileDownloader.getBaseFolder(requireNotNull(context)), fileNameFromUri)
        if (!fileToSend.exists()) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "openFile()() -> Cannot find $fileNameFromUri")
            }
            return null
        }

        return ParcelFileDescriptor.open(fileToSend, ParcelFileDescriptor.MODE_READ_ONLY)


    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            shareMatchId -> return "application/vnd.android.package-archive"
            else -> ""
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }


}