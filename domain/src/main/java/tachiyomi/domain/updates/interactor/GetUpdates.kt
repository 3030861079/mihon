package tachiyomi.domain.updates.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
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

    suspend fun await(read: Boolean, after: Instant): List<UpdatesWithRelations> {
        val hidden = getHiddenIds.await(HiddenItemType.Manga)
        val inHiddenSpace = hiddenMode.active.value
        return repository.awaitWithRead(read, after, limit = 500)
            .filter { (it.mangaId in hidden) == inHiddenSpace }
    }

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
            hiddenMode.active,
        ) { updates, hidden, inHiddenSpace ->
            updates.filter { (it.mangaId in hidden) == inHiddenSpace }
        }
    }

    fun subscribe(read: Boolean, after: Instant): Flow<List<UpdatesWithRelations>> {
        return combine(
            repository.subscribeWithRead(read, after, limit = 500),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            hiddenMode.active,
        ) { updates, hidden, inHiddenSpace ->
            updates.filter { (it.mangaId in hidden) == inHiddenSpace }
        }
    }
}
