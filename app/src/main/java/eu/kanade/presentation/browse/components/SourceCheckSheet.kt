package eu.kanade.presentation.browse.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel.CheckStatus
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Block
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.HourglassEmpty
import mihon.icons.materialsymbols.rounded.RemoveDone
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Result sheet for the batch source connectivity check.
 *
 * Lists every probed source with its status, round-trip time and, on failure, the underlying error
 * message so dead or Cloudflare-gated sources are easy to spot.
 */
@Composable
fun SourceCheckSheet(
    state: SourcesViewModel.CheckState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    var showOnlyFailed by remember { mutableStateOf(false) }

    val displayed = remember(state.results, showOnlyFailed) {
        if (showOnlyFailed) state.results.filter { it.status.isFailure } else state.results
    }

    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(MR.strings.source_check_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
            )

            Text(
                text = if (state.isRunning) {
                    stringResource(MR.strings.source_check_progress, state.completed, state.total)
                } else {
                    stringResource(MR.strings.source_check_summary, state.reachableCount, state.failedCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )

            if (state.isRunning) {
                LinearProgressIndicator(
                    progress = { state.completed.toFloat() / state.total.coerceAtLeast(1).toFloat() },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOnlyFailed = !showOnlyFailed }
                    .padding(horizontal = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = showOnlyFailed, onCheckedChange = { showOnlyFailed = it })
                Text(
                    text = stringResource(MR.strings.source_check_only_failed),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                items(items = displayed, key = { it.sourceId }) { result ->
                    SourceCheckRow(result)
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
                TextButton(onClick = onRetry, enabled = !state.isRunning) {
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
private fun SourceCheckRow(result: SourcesViewModel.CheckResult) {
    val icon: ImageVector
    val tint: Color
    when (result.status) {
        CheckStatus.Ok -> {
            icon = MaterialSymbols.Rounded.Check
            tint = MaterialTheme.colorScheme.primary
        }
        CheckStatus.Empty -> {
            icon = MaterialSymbols.Rounded.RemoveDone
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        CheckStatus.Timeout -> {
            icon = MaterialSymbols.Rounded.HourglassEmpty
            tint = MaterialTheme.colorScheme.error
        }
        CheckStatus.Error -> {
            icon = MaterialSymbols.Rounded.Error
            tint = MaterialTheme.colorScheme.error
        }
        CheckStatus.Skipped -> {
            icon = MaterialSymbols.Rounded.Block
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val subtitle = buildList {
        add(stringResource(result.status.labelRes()))
        if (result.elapsedMs > 0L) add("${result.elapsedMs} ms")
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
                text = result.name,
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

private fun CheckStatus.labelRes(): StringResource = when (this) {
    CheckStatus.Ok -> MR.strings.source_check_status_ok
    CheckStatus.Empty -> MR.strings.source_check_status_empty
    CheckStatus.Timeout -> MR.strings.source_check_status_timeout
    CheckStatus.Error -> MR.strings.source_check_status_error
    CheckStatus.Skipped -> MR.strings.source_check_status_skipped
}
