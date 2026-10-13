package eu.kanade.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.home.HiddenSpaceViewModel
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Picker for the hidden space. Everything here is checked/unchecked rather than moved, so the same
 * list both adds entries to the hidden space and takes them back out.
 */
@Composable
fun HiddenManagerSheet(
    viewModel: HiddenSpaceViewModel,
    onDismiss: () -> Unit,
) {
    val manga by viewModel.mangaCandidates.collectAsStateWithLifecycle()
    val readManga by viewModel.readNotInLibrary.collectAsStateWithLifecycle()
    val sources by viewModel.sourceCandidates.collectAsStateWithLifecycle()
    val hiddenManga by viewModel.hiddenMangaIds.collectAsStateWithLifecycle()
    val hiddenSources by viewModel.hiddenSourceIds.collectAsStateWithLifecycle()

    // Library entries first, then history-only manga, deduplicated by id.
    val allManga = remember(manga, readManga) {
        LinkedHashMap<Long, Manga>().apply {
            manga.forEach { put(it.manga.id, it.manga) }
            readManga.forEach { if (it.id !in this) put(it.id, it) }
        }.values.toList()
    }

    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(MR.strings.hidden_manager_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
            )
            Text(
                text = stringResource(MR.strings.hidden_manager_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
            Spacer(modifier = Modifier.size(MaterialTheme.padding.small))
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
            ) {
                item(key = "sources-header") {
                    SectionHeader(stringResource(MR.strings.hidden_manager_sources))
                }
                items(items = sources, key = { "source-${it.id}" }) { source ->
                    CheckRow(
                        label = source.name,
                        checked = source.id in hiddenSources,
                        onCheckedChange = { viewModel.setSourceHidden(source.id, it) },
                    )
                }

                item(key = "manga-header") {
                    SectionHeader(stringResource(MR.strings.hidden_manager_manga))
                }
                items(items = allManga, key = { "manga-${it.id}" }) { m ->
                    CheckRow(
                        label = m.title,
                        checked = m.id in hiddenManga,
                        onCheckedChange = { viewModel.setMangaHidden(m.id, it) },
                    )
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.small, vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(MR.strings.action_close))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.medium,
            top = MaterialTheme.padding.medium,
            bottom = MaterialTheme.padding.small,
        ),
    )
}

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
