package tachiyomi.domain.manga.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.retry
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.isVisibleInHiddenSpace
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.repository.MangaRepository
import kotlin.time.Duration.Companion.seconds

@Inject
class GetLibraryManga(
    private val mangaRepository: MangaRepository,
    private val getHiddenIds: GetHiddenIds,
    private val hiddenMode: HiddenModeManager,
) {

    /**
     * Full library, ignoring the hidden-space session. Use this for background workers that must
     * keep every entry up to date regardless of which space the UI is showing.
     */
    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getLibraryManga()
    }

    /**
     * Library entries visible in the current hidden-space session (main UI vs hidden space).
     */
    suspend fun awaitVisible(): List<LibraryManga> {
        val hiddenManga = getHiddenIds.await(HiddenItemType.Manga)
        val hiddenSources = getHiddenIds.await(HiddenItemType.Source)
        val inHiddenSpace = hiddenMode.active.value
        return mangaRepository.getLibraryManga()
            .filter {
                isVisibleInHiddenSpace(
                    mangaId = it.manga.id,
                    sourceId = it.manga.source,
                    hiddenMangaIds = hiddenManga,
                    hiddenSourceIds = hiddenSources,
                    inHiddenSpace = inHiddenSpace,
                )
            }
    }

    fun subscribe(): Flow<List<LibraryManga>> {
        return combine(
            mangaRepository.getLibraryMangaAsFlow(),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            getHiddenIds.subscribe(HiddenItemType.Source),
            hiddenMode.active,
        ) { manga, hiddenManga, hiddenSources, inHiddenSpace ->
            manga.filter {
                isVisibleInHiddenSpace(
                    mangaId = it.manga.id,
                    sourceId = it.manga.source,
                    hiddenMangaIds = hiddenManga,
                    hiddenSourceIds = hiddenSources,
                    inHiddenSpace = inHiddenSpace,
                )
            }
        }
            .retry {
                if (it is NullPointerException) {
                    delay(0.5.seconds)
                    true
                } else {
                    false
                }
            }.catch {
                this@GetLibraryManga.logcat(LogPriority.ERROR, it)
            }
    }
}
