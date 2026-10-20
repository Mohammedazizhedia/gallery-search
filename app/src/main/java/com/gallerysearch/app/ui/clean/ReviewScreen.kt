package com.gallerysearch.app.ui.clean

import android.app.Activity
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallerysearch.app.GalleryViewModel
import com.gallerysearch.app.R
import com.gallerysearch.app.data.MediaItem
import com.gallerysearch.app.data.TrashEntry
import com.gallerysearch.app.ui.components.LineIcon
import com.gallerysearch.app.ui.components.LineIconView
import com.gallerysearch.app.ui.theme.LocalTints
import kotlinx.coroutines.launch

/** Test build: one file per delete. Raise this once restore has been verified on the device. */
const val BATCH_LIMIT = 1

private fun toggle(sel: Set<Uri>, uri: Uri): Set<Uri> = when {
    uri in sel -> sel - uri
    BATCH_LIMIT == 1 -> setOf(uri)
    sel.size < BATCH_LIMIT -> sel + uri
    else -> sel
}

@Composable
fun ReviewScreen(vm: GalleryViewModel, duplicates: Boolean, onBack: () -> Unit) {
    val s by vm.scan.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var pending by remember { mutableStateOf<List<TrashEntry>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val removed = pending.map { Uri.parse(it.originalUri) }.toSet()
        if (r.resultCode == Activity.RESULT_OK) {
            vm.commitTrash(pending, removed)
            selected = selected - removed
        } else {
            vm.discardTrash(pending)
        }
        pending = emptyList()
    }

    val all = if (duplicates) s.dupGroups.flatten() else s.corrupt
    val chosen = all.filter { it.uri in selected }.take(BATCH_LIMIT)
    val chosenSize = Formatter.formatShortFileSize(ctx, chosen.sumOf { it.size })

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            Text(
                stringResource(if (duplicates) R.string.clean_dup else R.string.clean_corrupt),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (BATCH_LIMIT > 1 && !duplicates && s.corrupt.isNotEmpty()) {
                TextButton(onClick = { selected = s.corrupt.take(BATCH_LIMIT).map { it.uri }.toSet() }) {
                    Text(stringResource(R.string.select_all))
                }
            }
        }
        Text(
            stringResource(if (duplicates) R.string.hint_dup else R.string.hint_corrupt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (all.isEmpty()) {
                Text(stringResource(R.string.empty_review), modifier = Modifier.align(Alignment.Center))
            } else if (duplicates) {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(s.dupGroups, key = { it.first().uri.toString() }) { g ->
                        Column {
                            Text(
                                stringResource(R.string.group_label, g.size,
                                    Formatter.formatShortFileSize(ctx, g.first().size)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(g, key = { it.uri.toString() }) { m ->
                                    val on = m.uri in selected
                                    Thumb(
                                        m, on, keepLabel = BATCH_LIMIT > 1 && !on,
                                        modifier = Modifier.size(104.dp).clickable {
                                            val next = toggle(selected, m.uri)
                                            // never allow every copy of a group to be selected
                                            if (!g.all { it.uri in next }) selected = next
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(s.corrupt, key = { it.uri.toString() }) { m ->
                        val on = m.uri in selected
                        Column {
                            Thumb(
                                m, on, keepLabel = false,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                                    selected = toggle(selected, m.uri)
                                },
                            )
                            Text(m.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        notice?.let {
            Text(it, color = LocalTints.current.danger.fg, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        Button(
            onClick = {
                scope.launch {
                    val entries = vm.prepareTrash(chosen)
                    if (entries != null) {
                        pending = entries
                        launcher.launch(IntentSenderRequest.Builder(vm.deleteIntent(entries)).build())
                    }
                }
            },
            enabled = chosen.isNotEmpty() && !busy,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Text(
                if (busy) stringResource(R.string.preparing)
                else stringResource(R.string.trash_n, chosen.size, chosenSize),
            )
        }
    }
}

@Composable
private fun Thumb(m: MediaItem, selected: Boolean, keepLabel: Boolean, modifier: Modifier) {
    val tints = LocalTints.current
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(tints.danger.bg),
        contentAlignment = Alignment.Center,
    ) {
        // shown behind the picture: stays visible when a corrupted file cannot be previewed
        LineIconView(LineIcon.Broken, tints.danger.fg, 32.dp)
        AsyncImage(
            model = m.uri,
            contentDescription = m.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Box(
                Modifier.fillMaxSize().background(tints.danger.bg.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Delete, null, tint = tints.danger.fg) }
        } else if (keepLabel) {
            Text(
                stringResource(R.string.keep),
                color = tints.success.fg,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(tints.success.bg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
