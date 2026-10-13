package tachiyomi.domain.updates.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.isHiddenEntry
import tachiyomi.domain.hidden.isVisibleInHiddenSpace
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import kotlin.time.Instant

@Inject
class GetUpdates(
    private val repository: UpdatesRepository,
    private val getHiddenIds: GetHiddenIds,
    private val hiddenMode: HiddenModeManager,
) {

    /** Never surfaces hidden manga/sources — for one-shot callers that leave the app chrome. */
    suspend fun await(read: Boolean, after: Instant): List<UpdatesWithRelations> {
        val hiddenManga = getHiddenIds.await(HiddenItemType.Manga)
        val hiddenSources = getHiddenIds.await(HiddenItemType.Source)
        return repository.awaitWithRead(read, after, limit = 500)
            .filter { !isHiddenEntry(it.mangaId, it.sourceId, hiddenManga, hiddenSources) }
    }

    /** Updates tab: respect the current hidden-space session. */
    fun subscribe(
        instant: Instant,
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean,
        includedCategories: List<Long>,
        excludedCategories: List<Long>,
    ): Flow<List<UpdatesWithRelations>> {
        return combine(
            repository.subscribeAll(
                instant,
                limit = 500,
                unread = unread,
                started = started,
                bookmarked = bookmarked,
                hideExcludedScanlators = hideExcludedScanlators,
                includedCategories = includedCategories,
                excludedCategories = excludedCategories,
            ),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            getHiddenIds.subscribe(HiddenItemType.Source),
            hiddenMode.active,
        ) { updates, hiddenManga, hiddenSources, inHiddenSpace ->
            updates.filter {
                isVisibleInHiddenSpace(
                    mangaId = it.mangaId,
                    sourceId = it.sourceId,
                    hiddenMangaIds = hiddenManga,
                    hiddenSourceIds = hiddenSources,
                    inHiddenSpace = inHiddenSpace,
                )
            }
        }
    }

    /**
     * Home-screen widgets and other launcher surfaces: always exclude hidden manga/sources so
     * entering the hidden space cannot leak covers onto the desktop.
     */
    fun subscribe(read: Boolean, after: Instant): Flow<List<UpdatesWithRelations>> {
        return combine(
            repository.subscribeWithRead(read, after, limit = 500),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            getHiddenIds.subscribe(HiddenItemType.Source),
        ) { updates, hiddenManga, hiddenSources ->
            updates.filter { !isHiddenEntry(it.mangaId, it.sourceId, hiddenManga, hiddenSources) }
        }
    }
}
