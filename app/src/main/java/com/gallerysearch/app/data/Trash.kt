package com.gallerysearch.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class TrashEntry(
    val id: String,
    val originalUri: String,
    val name: String,
    val mime: String,
    val relPath: String,
    val isVideo: Boolean,
    val size: Long,
    val dateTaken: Long,
    val trashedAt: Long,
    val file: String,
    val pending: Boolean,
)

/**
 * The app's own trash. A verified copy is stored in app-private storage BEFORE the original is deleted,
 * so a deleted file can always be restored, even on phones whose system trash does not work.
 */
class TrashRepository(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "trash").apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val lock = Any()
    private val ttl = 30L * 24 * 60 * 60 * 1000

    fun list(): List<TrashEntry> = synchronized(lock) { readAll().filter { !it.pending } }

    /** Copies originals into the trash folder. Entries stay "pending" until the system delete is confirmed. */
    fun copyIn(items: List<MediaItem>): Pair<List<TrashEntry>, Int> = synchronized(lock) {
        val made = ArrayList<TrashEntry>()
        var failed = 0
        for (m in items) {
            val ext = m.name.substringAfterLast('.', "bin")
            val dest = File(dir, "${UUID.randomUUID()}.$ext")
            val ok = try {
                val input = ctx.contentResolver.openInputStream(m.uri)
                if (input == null) {
                    false
                } else {
                    input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                    dest.length() == m.size
                }
            } catch (e: Exception) {
                false
            }
            if (ok) {
                made += TrashEntry(
                    id = UUID.randomUUID().toString(),
                    originalUri = m.uri.toString(),
                    name = m.name,
                    mime = m.mime,
                    relPath = m.relPath,
                    isVideo = m.isVideo,
                    size = m.size,
                    dateTaken = m.dateTaken,
                    trashedAt = System.currentTimeMillis(),
                    file = dest.absolutePath,
                    pending = true,
                )
            } else {
                dest.delete()
                failed++
            }
        }
        if (made.isNotEmpty()) writeAll(readAll() + made)
        made to failed
    }

    fun commit(ids: Set<String>) = synchronized(lock) {
        val now = System.currentTimeMillis()
        writeAll(readAll().map { if (it.id in ids) it.copy(pending = false, trashedAt = now) else it })
    }

    fun discard(entries: List<TrashEntry>) = remove(entries)

    fun purge(entries: List<TrashEntry>) = remove(entries)

    private fun remove(entries: List<TrashEntry>) = synchronized(lock) {
        val ids = entries.map { it.id }.toSet()
        entries.forEach { File(it.file).delete() }
        writeAll(readAll().filterNot { it.id in ids })
    }

    /** Returns how many files could not be restored. Failed ones stay in the trash. */
    fun restore(entries: List<TrashEntry>): Int = synchronized(lock) {
        var failed = 0
        val done = ArrayList<TrashEntry>()
        for (e in entries) {
            if (restoreOne(e)) {
                done += e
                File(e.file).delete()
            } else {
                failed++
            }
        }
        val ids = done.map { it.id }.toSet()
        writeAll(readAll().filterNot { it.id in ids })
        failed
    }

    /** Runs at app start: settles interrupted deletions and removes entries older than 30 days. */
    fun maintenance() = synchronized(lock) {
        val now = System.currentTimeMillis()
        val keep = ArrayList<TrashEntry>()
        for (e in readAll()) {
            when {
                e.pending && originalExists(e) -> File(e.file).delete()
                e.pending -> keep += e.copy(pending = false, trashedAt = now)
                now - e.trashedAt > ttl -> File(e.file).delete()
                else -> keep += e
            }
        }
        writeAll(keep)
    }

    private fun originalExists(e: TrashEntry): Boolean = try {
        ctx.contentResolver.query(
            Uri.parse(e.originalUri), arrayOf(MediaStore.MediaColumns._ID), null, null, null,
        )?.use { it.count > 0 } ?: false
    } catch (ex: Exception) {
        false
    }

    private fun restoreOne(e: TrashEntry): Boolean {
        return try {
            val base = if (e.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val rel = when {
                e.relPath.isBlank() -> if (e.isVideo) "Movies/Restored/" else "Pictures/Restored/"
                e.relPath.endsWith("/") -> e.relPath
                else -> e.relPath + "/"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, e.name)
                if (e.mime.isNotBlank()) put(MediaStore.MediaColumns.MIME_TYPE, e.mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
                if (e.dateTaken > 0) put(MediaStore.MediaColumns.DATE_TAKEN, e.dateTaken)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(base, values) ?: return false
            val out = ctx.contentResolver.openOutputStream(uri)
            if (out == null) {
                ctx.contentResolver.delete(uri, null, null)
                return false
            }
            out.use { o -> File(e.file).inputStream().use { it.copyTo(o) } }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            ctx.contentResolver.update(uri, done, null, null)
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun readAll(): List<TrashEntry> {
        if (!index.exists()) return emptyList()
        return try {
            val arr = JSONArray(index.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeAll(list: List<TrashEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        val tmp = File(dir, "index.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(index)
    }

    private fun toJson(e: TrashEntry) = JSONObject()
        .put("id", e.id).put("originalUri", e.originalUri).put("name", e.name)
        .put("mime", e.mime).put("relPath", e.relPath).put("isVideo", e.isVideo)
        .put("size", e.size).put("dateTaken", e.dateTaken).put("trashedAt", e.trashedAt)
        .put("file", e.file).put("pending", e.pending)

    private fun fromJson(o: JSONObject) = TrashEntry(
        id = o.getString("id"),
        originalUri = o.getString("originalUri"),
        name = o.optString("name"),
        mime = o.optString("mime"),
        relPath = o.optString("relPath"),
        isVideo = o.optBoolean("isVideo"),
        size = o.optLong("size"),
        dateTaken = o.optLong("dateTaken"),
        trashedAt = o.optLong("trashedAt"),
        file = o.getString("file"),
        pending = o.optBoolean("pending"),
    )
}
