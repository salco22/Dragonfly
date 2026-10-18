package com.salco.dragonfly.feature.csvimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** Reads a document picked by the user; an interface so the ViewModel stays testable. */
interface FileSource {
    fun read(uri: String): ByteArray?
    fun displayName(uri: String): String?
}

internal class AndroidFileSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileSource {
    override fun read(uri: String): ByteArray? =
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }

    override fun displayName(uri: String): String? =
        context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface FileSourceModule {
    @Binds
    fun bindsFileSource(impl: AndroidFileSource): FileSource
}
