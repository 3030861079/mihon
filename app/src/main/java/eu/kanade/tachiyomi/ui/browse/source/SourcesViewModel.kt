package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.presentation.browse.SourceUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import java.util.TreeMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SourcesViewModel(
    private val getEnabledSources: GetEnabledSources,
    private val toggleSource: ToggleSource,
    private val toggleSourcePin: ToggleSourcePin,
    private val sourceManager: SourceManager,
) : ViewModel() {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val dialog = MutableStateFlow<Dialog?>(null)

    private val check = MutableStateFlow<CheckState?>(null)

    private var checkJob: Job? = null

    private val enabledSources = getEnabledSources.subscribe()
        .catch {
            logcat(LogPriority.ERROR, it)
            _events.send(Event.FailedFetchingSources)
        }
        .map(::toSourceUiModels)

    val state: StateFlow<State> = combine(
        enabledSources,
        dialog,
        check,
    ) { items, dialog, check ->
        State(dialog = dialog, isLoading = false, items = items, check = check)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    private fun toSourceUiModels(sources: List<Source>): List<SourceUiModel> {
        val map = TreeMap<String, MutableList<Source>> { d1, d2 ->
            // Sources without a lang defined will be placed at the end
            when {
                d1 == LAST_USED_KEY && d2 != LAST_USED_KEY -> -1
                d2 == LAST_USED_KEY && d1 != LAST_USED_KEY -> 1
                d1 == PINNED_KEY && d2 != PINNED_KEY -> -1
                d2 == PINNED_KEY && d1 != PINNED_KEY -> 1
                d1 == "" && d2 != "" -> 1
                d2 == "" && d1 != "" -> -1
                else -> d1.compareTo(d2)
            }
        }
        val byLang = sources.groupByTo(map) {
            when {
                it.isUsedLast -> LAST_USED_KEY
                Pin.Actual in it.pin -> PINNED_KEY
                else -> it.lang
            }
        }

        return byLang.flatMap {
            listOf(
                SourceUiModel.Header(it.key),
                *it.value.map { source ->
                    SourceUiModel.Item(source)
                }.toTypedArray(),
            )
        }
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun togglePin(source: Source) {
        toggleSourcePin.await(source)
    }

    fun showSourceDialog(source: Source) {
        dialog.update { Dialog(source) }
    }

    fun closeDialog() {
        dialog.update { null }
    }

    // ---------------------------------------------------------------------------------------------
    // Source connectivity check
    // ---------------------------------------------------------------------------------------------

    /**
     * Probes every enabled source by requesting its first popular page.
     *
     * Sources block on the network and some of them are Cloudflare-gated or simply dead, so the
     * request is bounded by [CHECK_TIMEOUT] and only [CHECK_PARALLELISM] sources are probed at once.
     */
    fun startSourceCheck() {
        if (checkJob?.isActive == true) return

        checkJob = viewModelScope.launch {
            val sources = state.value.items
                .filterIsInstance<SourceUiModel.Item>()
                .map { it.source }
                .distinctBy { it.id }
            if (sources.isEmpty()) return@launch

            check.value = CheckState(isRunning = true, total = sources.size)

            val semaphore = Semaphore(CHECK_PARALLELISM)
            coroutineScope {
                sources
                    .map { source ->
                        async {
                            val result = semaphore.withPermit { probe(source) }
                            check.update { current ->
                                current?.copy(
                                    completed = current.completed + 1,
                                    results = current.results + result,
                                )
                            }
                        }
                    }
                    .awaitAll()
            }

            check.update { it?.copy(isRunning = false) }
        }
    }

    private suspend fun probe(source: Source): CheckResult {
        val mark = TimeSource.Monotonic.markNow()
        fun elapsed() = mark.elapsedNow().inWholeMilliseconds

        fun result(status: CheckStatus, detail: String? = null) = CheckResult(
            sourceId = source.id,
            name = source.name,
            lang = source.lang,
            status = status,
            elapsedMs = elapsed(),
            detail = detail,
        )

        val apiSource = sourceManager.get(source.id)
        if (apiSource == null || apiSource.isLocal()) {
            return result(CheckStatus.Skipped)
        }

        return try {
            val page = withContext(Dispatchers.IO) {
                withTimeout(CHECK_TIMEOUT) {
                    apiSource.getPopularManga(1)
                }
            }
            if (page.mangas.isEmpty()) {
                result(CheckStatus.Empty)
            } else {
                result(CheckStatus.Ok, page.mangas.size.toString())
            }
        } catch (e: TimeoutCancellationException) {
            result(CheckStatus.Timeout)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logcat(LogPriority.DEBUG, e) { "Connectivity check failed for ${source.name}" }
            val message = e.message?.takeIf { it.isNotBlank() }?.take(160) ?: e::class.simpleName.orEmpty()
            result(CheckStatus.Error, message)
        }
    }

    /** Dismisses the results and stops an in-flight check. */
    fun dismissSourceCheck() {
        checkJob?.cancel()
        checkJob = null
        check.value = null
    }

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    data class Dialog(val source: Source)

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val isLoading: Boolean = true,
        val items: List<SourceUiModel> = listOf(),
        val check: CheckState? = null,
    ) {
        val isEmpty = items.isEmpty()
    }

    @Immutable
    data class CheckState(
        val isRunning: Boolean = false,
        val total: Int = 0,
        val completed: Int = 0,
        val results: List<CheckResult> = emptyList(),
    ) {
        val reachableCount: Int get() = results.count { it.status == CheckStatus.Ok }
        val failedCount: Int get() = results.count { it.status.isFailure }
    }

    @Immutable
    data class CheckResult(
        val sourceId: Long,
        val name: String,
        val lang: String,
        val status: CheckStatus,
        val elapsedMs: Long = 0L,
        val detail: String? = null,
    )

    enum class CheckStatus(val isFailure: Boolean) {
        Ok(isFailure = false),
        Empty(isFailure = false),
        Timeout(isFailure = true),
        Error(isFailure = true),
        Skipped(isFailure = false),
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"

        private val CHECK_TIMEOUT = 15.seconds
        private const val CHECK_PARALLELISM = 4
    }
}
