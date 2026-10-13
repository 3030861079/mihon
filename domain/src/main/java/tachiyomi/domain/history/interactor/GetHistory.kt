package tachiyomi.domain.history.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

@Inject
class GetHistory(
    private val repository: HistoryRepository,
    private val getHiddenIds: GetHiddenIds,
    private val hiddenMode: HiddenModeManager,
) {

    suspend fun await(mangaId: Long): List<History> {
        return repository.getHistoryByMangaId(mangaId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return combine(
            repository.getHistory(query),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            hiddenMode.active,
        ) { history, hidden, inHiddenSpace ->
            history.filter { (it.mangaId in hidden) == inHiddenSpace }
        }
    }
}
