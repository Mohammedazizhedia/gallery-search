package com.gallerysearch.app.ui.clean

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallerysearch.app.GalleryViewModel
import com.gallerysearch.app.R
import com.gallerysearch.app.ui.components.LineIcon
import com.gallerysearch.app.ui.components.LineIconView
import com.gallerysearch.app.ui.theme.LocalTints
import com.gallerysearch.app.ui.theme.Tint

@Composable
fun CleanScreen(vm: GalleryViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = mode != 0) { mode = 0 }
    when (mode) {
        1 -> ReviewScreen(vm, duplicates = true) { mode = 0 }
        2 -> ReviewScreen(vm, duplicates = false) { mode = 0 }
        3 -> TrashScreen(vm) { mode = 0 }
        else -> CleanHome(vm) { mode = it }
    }
}

@Composable
private fun CleanHome(vm: GalleryViewModel, open: (Int) -> Unit) {
    val s by vm.scan.collectAsStateWithLifecycle()
    val trash by vm.trash.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val tints = LocalTints.current
    val trashSize = Formatter.formatShortFileSize(ctx, trash.sumOf { it.size })
    val size = Formatter.formatShortFileSize(ctx, s.dupBytes)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text(stringResource(R.string.clean_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(tints.accent.bg).padding(14.dp),
        ) {
            Text(stringResource(R.string.clean_save), color = tints.accent.fg, style = MaterialTheme.typography.bodySmall)
            Text(size, color = tints.accent.fg, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.clean_note), color = tints.accent.fg, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        CleanRow(
            LineIcon.Dup, tints.accent, stringResource(R.string.clean_dup),
            if (s.done) stringResource(R.string.files_size, s.dupExtra, size) else "—",
            onClick = if (s.done && s.dupGroups.isNotEmpty()) ({ open(1) }) else null,
        )
        CleanRow(
            LineIcon.Broken, tints.danger, stringResource(R.string.clean_corrupt),
            if (s.done) stringResource(R.string.files_count, s.corrupt.size) else "—",
            onClick = if (s.done && s.corrupt.isNotEmpty()) ({ open(2) }) else null,
        )
        CleanRow(LineIcon.Blur, tints.warning, stringResource(R.string.clean_blur),
            stringResource(R.string.clean_soon), onClick = null)
        CleanRow(LineIcon.Trash, tints.pro, stringResource(R.string.clean_trash),
            stringResource(R.string.files_size, trash.size, trashSize), onClick = { open(3) })
        Spacer(Modifier.height(12.dp))
        if (s.running) {
            LinearProgressIndicator(
                progress = { if (s.total > 0) s.progress.toFloat() / s.total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.scan_progress, s.progress, s.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
        }
        Button(onClick = { vm.scanClean() }, enabled = !s.running, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (s.running) R.string.scanning else R.string.scan))
        }
    }
}

@Composable
private fun CleanRow(icon: LineIcon, tint: Tint, title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(tint.bg),
            contentAlignment = Alignment.Center,
        ) { LineIconView(icon, tint.fg, 36.dp) }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
