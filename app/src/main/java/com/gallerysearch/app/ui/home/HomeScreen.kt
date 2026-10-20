package com.gallerysearch.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallerysearch.app.GalleryViewModel
import com.gallerysearch.app.R
import com.gallerysearch.app.ui.components.LineIcon
import com.gallerysearch.app.ui.components.LineIconView
import com.gallerysearch.app.ui.theme.LocalTints
import com.gallerysearch.app.ui.theme.Tint

@Composable
fun HomeScreen(vm: GalleryViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle()
    val shown = remember(items, q) {
        if (q.isBlank()) items else items.filter { it.name.contains(q, ignoreCase = true) }
    }
    val tints = LocalTints.current
    val videoCount = remember(items) { items.count { it.isVideo } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(stringResource(R.string.home_sub), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = q,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = CircleShape,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { /* offline voice search: stage 6 */ }) {
                            Icon(Icons.Outlined.Mic, stringResource(R.string.voice), tint = tints.accent.fg)
                        }
                    },
                )
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.smart_cats), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                val soon = stringResource(R.string.cat_soon)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryTile(Modifier.weight(1f), LineIcon.Car, stringResource(R.string.cat_cars), soon, tints.accent)
                    CategoryTile(Modifier.weight(1f), LineIcon.Face, stringResource(R.string.cat_people), soon, tints.pro)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryTile(Modifier.weight(1f), LineIcon.Receipt, stringResource(R.string.cat_docs), soon, tints.warning)
                    CategoryTile(Modifier.weight(1f), LineIcon.Scenery, stringResource(R.string.cat_nature), soon, tints.success)
                }
                Spacer(Modifier.height(10.dp))
                CategoryTile(
                    Modifier.fillMaxWidth(), LineIcon.Video, stringResource(R.string.cat_videos),
                    stringResource(R.string.videos_count, videoCount), tints.danger,
                )
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }
        }
        items(shown, key = { it.uri.toString() }) { m ->
            AsyncImage(
                model = m.uri,
                contentDescription = m.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)),
            )
        }
    }
}

@Composable
private fun CategoryTile(modifier: Modifier, icon: LineIcon, title: String, subtitle: String, tint: Tint) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(tint.bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LineIconView(icon, tint.fg)
        Text(title, style = MaterialTheme.typography.titleMedium, color = tint.fg)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tint.fg)
    }
}
