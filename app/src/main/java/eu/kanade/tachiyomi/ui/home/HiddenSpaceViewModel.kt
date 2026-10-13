package eu.kanade.tachiyomi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.interactor.ToggleHidden
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Backs the hidden space: the in-memory mode flag plus the candidate lists used by the manager
 * sheet.
 *
 * The candidate lists come straight from the repositories, i.e. unfiltered, so entries can be
 * checked and unchecked even while one of the two modes is active.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HiddenSpaceViewModel(
    private val hiddenMode: HiddenModeManager,
    private val getHiddenIds: GetHiddenIds,
    private val toggleHidden: ToggleHidden,
    private val mangaRepository: MangaRepository,
    sourceManager: SourceManager,
) : ViewModel() {

    val active: StateFlow<Boolean> = hiddenMode.active

    val mangaCandidates: StateFlow<List<LibraryManga>> = mangaRepository.getLibraryMangaAsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    /** Manga with reading history but outside the library, so history entries can be hidden per manga. */
    val readNotInLibrary: StateFlow<List<Manga>> = flow {
        emit(mangaRepository.getReadMangaNotInLibrary())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val sourceCandidates: StateFlow<List<Source>> = sourceManager.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val hiddenMangaIds: StateFlow<Set<Long>> = getHiddenIds.subscribe(HiddenItemType.Manga)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    val hiddenSourceIds: StateFlow<Set<Long>> = getHiddenIds.subscribe(HiddenItemType.Source)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    fun enter() = hiddenMode.enter()

    fun exit() = hiddenMode.exit()

    fun setMangaHidden(mangaId: Long, hidden: Boolean) {
        viewModelScope.launch { toggleHidden.manga(mangaId, hidden) }
    }

    fun setSourceHidden(sourceId: Long, hidden: Boolean) {
        viewModelScope.launch { toggleHidden.source(sourceId, hidden) }
    }
}
