package com.gallerysearch.app

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gallerysearch.app.data.Analyzer
import com.gallerysearch.app.data.MediaItem
import com.gallerysearch.app.data.MediaRepository
import com.gallerysearch.app.data.TrashEntry
import com.gallerysearch.app.data.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanState(
    val running: Boolean = false,
    val done: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val dupGroups: List<List<MediaItem>> = emptyList(),
    val corrupt: List<MediaItem> = emptyList(),
) {
    val dupExtra: Int get() = dupGroups.sumOf { it.size - 1 }
    val dupBytes: Long get() = dupGroups.sumOf { it.first().size * (it.size - 1) }
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    val items = MutableStateFlow<List<MediaItem>>(emptyList())
    val query = MutableStateFlow("")
    val scan = MutableStateFlow(ScanState())
    val trash = MutableStateFlow<List<TrashEntry>>(emptyList())
    val busy = MutableStateFlow(false)
    val notice = MutableStateFlow<String?>(null)
    private val trashRepo = TrashRepository(app)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            trashRepo.maintenance()
            trash.value = trashRepo.list()
        }
    }

    fun load() {
        viewModelScope.launch { items.value = MediaRepository.load(getApplication()) }
    }

    fun scanClean() {
        if (scan.value.running) return
        val all = items.value
        scan.value = ScanState(running = true, total = all.size)
        viewModelScope.launch(Dispatchers.Default) {
            val ctx = getApplication<Application>()
            var done = 0
            val corrupt = ArrayList<MediaItem>()
            for (m in all) {
                if (Analyzer.isCorrupt(ctx, m)) corrupt += m
                done++
                if (done % 20 == 0) scan.update { it.copy(progress = done) }
            }
            val bad = corrupt.map { it.uri }.toSet()
            val groups = Analyzer.sameSizeGroups(all.filter { it.uri !in bad })
            val total = all.size + groups.sumOf { it.size }
            scan.update { it.copy(total = total, progress = done) }
            val dups = Analyzer.findDuplicates(ctx, groups) {
                done++
                if (done % 5 == 0) scan.update { s -> s.copy(progress = done) }
            }
            scan.value = ScanState(
                done = true,
                progress = total,
                total = total,
                dupGroups = dups.sortedByDescending { it.first().size * it.size },
                corrupt = corrupt,
            )
        }
    }

    /** Step 1: store a verified copy in the app trash. Returns null when nothing could be backed up. */
    suspend fun prepareTrash(list: List<MediaItem>): List<TrashEntry>? {
        busy.value = true
        notice.value = null
        val (entries, failed) = withContext(Dispatchers.IO) { trashRepo.copyIn(list) }
        busy.value = false
        if (failed > 0) {
            notice.value = getApplication<Application>().getString(R.string.notice_copy_failed, failed)
        }
        return if (entries.isEmpty()) null else entries
    }

    /** Step 2: the system dialog that deletes the originals (only those that were backed up). */
    fun deleteIntent(entries: List<TrashEntry>): IntentSender =
        MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            entries.map { Uri.parse(it.originalUri) },
        ).intentSender

    fun commitTrash(entries: List<TrashEntry>, removed: Set<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            trashRepo.commit(entries.map { it.id }.toSet())
            trash.value = trashRepo.list()
        }
        onTrashed(removed)
    }

    fun discardTrash(entries: List<TrashEntry>) {
        viewModelScope.launch(Dispatchers.IO) { trashRepo.discard(entries) }
    }

    fun restore(sel: List<TrashEntry>) {
        viewModelScope.launch {
            busy.value = true
            notice.value = null
            val failed = withContext(Dispatchers.IO) { trashRepo.restore(sel) }
            trash.value = withContext(Dispatchers.IO) { trashRepo.list() }
            busy.value = false
            if (failed > 0) {
                notice.value = getApplication<Application>().getString(R.string.notice_restore_failed, failed)
            }
            load()
        }
    }

    fun purge(sel: List<TrashEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            trashRepo.purge(sel)
            trash.value = trashRepo.list()
        }
    }

    fun onTrashed(removed: Set<Uri>) {
        items.update { l -> l.filter { it.uri !in removed } }
        scan.update { s ->
            s.copy(
                dupGroups = s.dupGroups.map { g -> g.filter { it.uri !in removed } }.filter { it.size > 1 },
                corrupt = s.corrupt.filter { it.uri !in removed },
            )
        }
    }
}
