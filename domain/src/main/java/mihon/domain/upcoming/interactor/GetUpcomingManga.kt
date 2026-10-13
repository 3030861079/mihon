package mihon.domain.upcoming.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.isVisibleInHiddenSpace
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

@Inject
class GetUpcomingManga(
    private val mangaRepository: MangaRepository,
    private val getHiddenIds: GetHiddenIds,
    private val hiddenMode: HiddenModeManager,
) {

    private val includedStatuses = setOf(
        SManga.ONGOING.toLong(),
        SManga.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Manga>> {
        return combine(
            mangaRepository.getUpcomingManga(
                includedStatuses,
                excludedCategories = excludedCategories,
                includedCategories = includedCategories,
            ),
            getHiddenIds.subscribe(HiddenItemType.Manga),
            getHiddenIds.subscribe(HiddenItemType.Source),
            hiddenMode.active,
        ) { manga, hiddenManga, hiddenSources, inHiddenSpace ->
            manga.filter {
                isVisibleInHiddenSpace(
                    mangaId = it.id,
                    sourceId = it.source,
                    hiddenMangaIds = hiddenManga,
                    hiddenSourceIds = hiddenSources,
                    inHiddenSpace = inHiddenSpace,
                )
            }
        }
    }
}
