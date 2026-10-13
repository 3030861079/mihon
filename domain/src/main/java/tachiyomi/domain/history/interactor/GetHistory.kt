package tachiyomi.domain.history.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.isVisibleInHiddenSpace
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
            getHiddenIds.subscribe(HiddenItemType.Source),
            hiddenMode.active,
        ) { history, hiddenManga, hiddenSources, inHiddenSpace ->
            history.filter {
                isVisibleInHiddenSpace(
                    mangaId = it.mangaId,
                    sourceId = it.coverData.sourceId,
                    hiddenMangaIds = hiddenManga,
                    hiddenSourceIds = hiddenSources,
                    inHiddenSpace = inHiddenSpace,
                )
            }
        }
    }
}
