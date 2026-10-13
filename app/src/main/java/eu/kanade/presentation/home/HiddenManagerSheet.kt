package eu.kanade.presentation.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.home.HiddenSpaceViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Picker for the hidden space. Everything here is checked/unchecked rather than moved, so the same
 * list both adds entries to the hidden space and takes them back out.
 *
 * Multi-language catalogues that share a name (e.g. Manga Million) are collapsed into one row so
 * you can hide every locale with a single tap; expand the row to tweak individual languages.
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

    val sourceGroups = remember(sources) {
        sources
            .groupBy { it.name.trim().lowercase() }
            .values
            .map { members ->
                SourceGroup(
                    name = members.first().name,
                    sources = members.sortedBy { it.lang.lowercase() },
                )
            }
            .sortedBy { it.name.lowercase() }
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
                items(items = sourceGroups, key = { "source-group-${it.name}" }) { group ->
                    SourceGroupRow(
                        group = group,
                        hiddenSourceIds = hiddenSources,
                        onToggleAll = { hidden ->
                            viewModel.setSourcesHidden(group.sources.map { it.id }, hidden)
                        },
                        onToggleOne = { sourceId, hidden ->
                            viewModel.setSourceHidden(sourceId, hidden)
                        },
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

private data class SourceGroup(
    val name: String,
    val sources: List<Source>,
) {
    val ids: List<Long> get() = sources.map { it.id }
}

@Composable
private fun SourceGroupRow(
    group: SourceGroup,
    hiddenSourceIds: Set<Long>,
    onToggleAll: (Boolean) -> Unit,
    onToggleOne: (Long, Boolean) -> Unit,
) {
    val hiddenCount = group.sources.count { it.id in hiddenSourceIds }
    val state = when (hiddenCount) {
        0 -> ToggleableState.Off
        group.sources.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val multiLang = group.sources.size > 1
    var expanded by rememberSaveable(group.name) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onToggleAll(state != ToggleableState.On)
                }
                .padding(horizontal = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = state,
                onClick = { onToggleAll(state != ToggleableState.On) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (multiLang) {
                    val langs = group.sources
                        .map { it.lang.ifBlank { "?" }.uppercase() }
                        .distinct()
                        .joinToString(", ")
                    Text(
                        text = stringResource(
                            MR.strings.hidden_manager_source_langs,
                            group.sources.size,
                            langs,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (group.sources.first().lang.isNotBlank()) {
                    Text(
                        text = group.sources.first().lang.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (multiLang) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) {
                            MaterialSymbols.Rounded.KeyboardArrowUp
                        } else {
                            MaterialSymbols.Rounded.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }
            }
        }

        AnimatedVisibility(visible = multiLang && expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.sources.forEach { source ->
                    CheckRow(
                        label = source.lang.ifBlank { source.id.toString() }.uppercase(),
                        checked = source.id in hiddenSourceIds,
                        onCheckedChange = { onToggleOne(source.id, it) },
                        indent = true,
                    )
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
    indent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                start = if (indent) MaterialTheme.padding.large + MaterialTheme.padding.medium
                else MaterialTheme.padding.small,
                end = MaterialTheme.padding.small,
            ),
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
