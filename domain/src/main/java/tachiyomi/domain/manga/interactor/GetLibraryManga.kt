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

    suspend fun await(): List<LibraryManga> {
        val hidden = getHiddenIds.await(HiddenItemType.Manga)
        val inHiddenSpace = hiddenMode.active.value
        return mangaRepository.getLibraryManga()
            .filter { (it.manga.id in hidden) == inHiddenSpace }
    }

    fun subscribe(): Flow<List<LibraryManga>> {
        return combine(
            mangaRepository.getLibraryMangaAsFlow(),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            hiddenMode.active,
        ) { manga, hidden, inHiddenSpace ->
            manga.filter { (it.manga.id in hidden) == inHiddenSpace }
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
