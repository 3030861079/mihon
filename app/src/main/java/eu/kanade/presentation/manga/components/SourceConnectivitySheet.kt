package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.manga.ImageResult
import eu.kanade.tachiyomi.ui.manga.SourceCheckError
import eu.kanade.tachiyomi.ui.manga.SourceCheckState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Block
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.HourglassEmpty
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Result sheet for the per-manga connectivity check.
 *
 * Unlike the batch source check, this one is end to end: it lists the outcome of actually
 * downloading the first few images of the manga's latest chapter, so a source that serves a
 * chapter listing but 403s on every image is reported as broken rather than reachable.
 */
@Composable
fun SourceConnectivitySheet(
    state: SourceCheckState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val running = state as? SourceCheckState.Running
    val done = state as? SourceCheckState.Done

    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(MR.strings.manga_check_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
            )

            val summary = when {
                running != null -> stringResource(
                    MR.strings.source_check_progress,
                    running.completed,
                    running.total.coerceAtLeast(1),
                )

                done != null && done.error == null -> stringResource(
                    MR.strings.source_check_summary,
                    done.okCount,
                    done.failedCount,
                )

                else -> null
            }
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }

            val chapterName = running?.chapterName ?: done?.chapterName
            if (chapterName != null) {
                Text(
                    text = stringResource(MR.strings.manga_check_chapter, chapterName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = 4.dp,
                    ),
                )
            }

            val totalPages = running?.totalPages ?: done?.totalPages ?: 0
            val results = running?.results ?: done?.results.orEmpty()
            val probing = running?.total ?: results.size
            if (totalPages > 0 && probing > 0) {
                Text(
                    text = stringResource(MR.strings.manga_check_pages, totalPages, probing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }

            if (running != null) {
                LinearProgressIndicator(
                    progress = { running.completed.toFloat() / running.total.coerceAtLeast(1).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.small,
                        ),
                )
            } else {
                Spacer(modifier = Modifier.size(MaterialTheme.padding.small))
            }

            val error = done?.error
            if (error != null) {
                Text(
                    text = stringResource(error.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
                done.errorDetail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    )
                }
            }

            if (results.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(top = MaterialTheme.padding.small))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(items = results, key = { it.index }) { result ->
                        ImageResultRow(result)
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.small, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRetry, enabled = running == null) {
                    Text(text = stringResource(MR.strings.action_retry))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(MR.strings.action_close))
                }
            }
        }
    }
}

@Composable
private fun ImageResultRow(result: ImageResult) {
    val icon: ImageVector
    val tint: Color
    if (result.ok) {
        icon = MaterialSymbols.Rounded.Check
        tint = MaterialTheme.colorScheme.primary
    } else {
        when (result.error) {
            SourceCheckError.Timeout -> {
                icon = MaterialSymbols.Rounded.HourglassEmpty
                tint = MaterialTheme.colorScheme.error
            }

            SourceCheckError.NotImage -> {
                icon = MaterialSymbols.Rounded.Block
                tint = MaterialTheme.colorScheme.error
            }

            else -> {
                icon = MaterialSymbols.Rounded.Error
                tint = MaterialTheme.colorScheme.error
            }
        }
    }

    val subtitle = buildList {
        add(
            stringResource(
                if (result.ok) MR.strings.source_check_status_ok else MR.strings.source_check_status_error,
            ),
        )
        result.code?.let { add("HTTP $it") }
        if (result.bytes > 0) add("${result.bytes / 1024} KB")
        if (result.elapsedMs > 0) add("${result.elapsedMs} ms")
        if (result.error == SourceCheckError.NotImage) add(stringResource(MR.strings.manga_check_not_image))
        result.detail?.let { add(it) }
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.manga_check_image_n, result.index + 1),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun SourceCheckError.labelRes(): StringResource = when (this) {
    SourceCheckError.Unsupported -> MR.strings.manga_check_unsupported
    SourceCheckError.NoChapters -> MR.strings.manga_check_no_chapters
    SourceCheckError.NoPages -> MR.strings.manga_check_no_pages
    SourceCheckError.FetchFailed -> MR.strings.manga_check_fetch_failed
    SourceCheckError.Timeout -> MR.strings.source_check_status_timeout
    SourceCheckError.HttpError -> MR.strings.source_check_status_error
    SourceCheckError.NotImage -> MR.strings.manga_check_not_image
    SourceCheckError.Error -> MR.strings.source_check_status_error
}
