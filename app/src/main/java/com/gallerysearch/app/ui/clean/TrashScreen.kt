package com.gallerysearch.app.ui.clean

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.gallerysearch.app.ui.components.LineIcon
import com.gallerysearch.app.ui.components.LineIconView
import com.gallerysearch.app.ui.theme.LocalTints
import java.io.File

@Composable
fun TrashScreen(vm: GalleryViewModel, onBack: () -> Unit) {
    val entries by vm.trash.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val tints = LocalTints.current

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirm by remember { mutableStateOf(false) }
    val chosen = entries.filter { it.id in selected }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            Text(stringResource(R.string.trash_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(
            stringResource(R.string.trash_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (entries.isEmpty()) {
                Text(stringResource(R.string.empty_review), modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries, key = { it.id }) { e ->
                        val on = e.id in selected
                        Column {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tints.accent.bg)
                                    .clickable { selected = if (on) selected - e.id else selected + e.id },
                                contentAlignment = Alignment.Center,
                            ) {
                                LineIconView(LineIcon.Scenery, tints.accent.fg, 32.dp)
                                AsyncImage(
                                    model = File(e.file),
                                    contentDescription = e.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (on) {
                                    Box(
                                        Modifier.fillMaxSize().background(tints.accent.bg.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Outlined.Check, null, tint = tints.accent.fg) }
                                }
                            }
                            Text(e.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                            Text(Formatter.formatShortFileSize(ctx, e.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        notice?.let {
            Text(it, color = tints.danger.fg, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { vm.restore(chosen); selected = emptySet() },
                enabled = chosen.isNotEmpty() && !busy,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.restore_n, chosen.size)) }
            OutlinedButton(
                onClick = { confirm = true },
                enabled = chosen.isNotEmpty() && !busy,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.purge_n, chosen.size)) }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.purge_title)) },
            text = { Text(stringResource(R.string.purge_body)) },
            confirmButton = {
                TextButton(onClick = { vm.purge(chosen); selected = emptySet(); confirm = false }) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
