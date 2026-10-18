package com.salco.dragonfly.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.salco.dragonfly.core.work.AutoBackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** Android document and scheduling access, kept behind an interface so the ViewModel stays testable. */
interface DocumentAccess {
    fun openOutput(uri: String): OutputStream?
    fun openInput(uri: String): InputStream?

    /** Keeps write access to a chosen folder across restarts. */
    fun persistFolderAccess(uri: String)
    fun releaseFolderAccess(uri: String)
    fun folderName(uri: String): String?
    fun scheduleAutoBackup()
    fun cancelAutoBackup()
}

internal class AndroidDocumentAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentAccess {
    private val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    override fun openOutput(uri: String): OutputStream? = context.contentResolver.openOutputStream(Uri.parse(uri), "wt")
    override fun openInput(uri: String): InputStream? = context.contentResolver.openInputStream(Uri.parse(uri))

    override fun persistFolderAccess(uri: String) = context.contentResolver.takePersistableUriPermission(Uri.parse(uri), flags)

    override fun releaseFolderAccess(uri: String) {
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), flags) }
    }

    override fun folderName(uri: String): String? = DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name

    override fun scheduleAutoBackup() = AutoBackupWorker.schedule(context)
    override fun cancelAutoBackup() = AutoBackupWorker.cancel(context)
}
