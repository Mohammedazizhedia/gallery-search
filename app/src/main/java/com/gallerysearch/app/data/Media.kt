package com.gallerysearch.app.data

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val mime: String = "",
    val relPath: String = "",
    val dateTaken: Long = 0L,
)

object MediaRepository {
    suspend fun load(ctx: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        (query(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false) +
            query(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true))
            .sortedByDescending { it.dateAdded }
    }

    private fun query(ctx: Context, base: Uri, video: Boolean): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_TAKEN,
        )
        ctx.contentResolver.query(base, projection, null, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iDate = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val iMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val iRel = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val iTaken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    isVideo = video,
                    name = c.getString(iName) ?: "",
                    size = c.getLong(iSize),
                    dateAdded = c.getLong(iDate),
                    mime = c.getString(iMime) ?: "",
                    relPath = c.getString(iRel) ?: "",
                    dateTaken = c.getLong(iTaken),
                )
            }
        }
        return out
    }
}

object Analyzer {
    /** Files with identical size are the only candidates, so we hash far fewer files. */
    fun sameSizeGroups(items: List<MediaItem>): List<List<MediaItem>> =
        items.filter { it.size > 0 }
            .groupBy { it.size }
            .values.filter { it.size > 1 }

    /** Exact duplicates: SHA-256 inside same-size groups only. */
    fun findDuplicates(ctx: Context, groups: List<List<MediaItem>>, onHashed: () -> Unit): List<List<MediaItem>> =
        groups.flatMap { group ->
            group.mapNotNull { item ->
                val h = sha256(ctx, item.uri)
                onHashed()
                h?.let { it to item }
            }
                .groupBy({ it.first }, { it.second })
                .values.filter { it.size > 1 }
        }

    /** A file is flagged when it is empty or its header cannot be decoded. */
    fun isCorrupt(ctx: Context, item: MediaItem): Boolean {
        if (item.size <= 0L) return true
        return try {
            if (item.isVideo) {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(ctx, item.uri)
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) == null
                } finally {
                    r.release()
                }
            } else {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val s = ctx.contentResolver.openInputStream(item.uri) ?: return true
                s.use { BitmapFactory.decodeStream(it, null, o) }
                o.outWidth <= 0 || o.outHeight <= 0
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun sha256(ctx: Context, uri: Uri): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val s = ctx.contentResolver.openInputStream(uri) ?: return null
            s.use {
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = it.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
