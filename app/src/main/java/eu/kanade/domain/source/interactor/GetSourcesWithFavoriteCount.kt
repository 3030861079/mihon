package eu.kanade.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.hidden.HiddenModeManager
import tachiyomi.domain.hidden.interactor.GetHiddenIds
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal
import java.util.Collections

@Inject
class GetSourcesWithFavoriteCount(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
    private val getHiddenIds: GetHiddenIds,
    private val hiddenMode: HiddenModeManager,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return combine(
            preferences.migrationSortingDirection.changes(),
            preferences.migrationSortingMode.changes(),
            repository.getSourcesWithFavoriteCount(),
            getHiddenIds.subscribe(HiddenItemType.Source),
            hiddenMode.active,
        ) { direction, mode, list, hiddenSources, inHiddenSpace ->
            list
                .filterNot { it.first.isLocal() }
                .filter { (it.first.id in hiddenSources) == inHiddenSpace }
                .sortedWith(sortFn(direction, mode))
        }
    }

    private fun sortFn(
        direction: SetMigrateSorting.Direction,
        sorting: SetMigrateSorting.Mode,
    ): java.util.Comparator<Pair<Source, Long>> {
        val sortFn: (Pair<Source, Long>, Pair<Source, Long>) -> Int = { a, b ->
            when (sorting) {
                SetMigrateSorting.Mode.ALPHABETICAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.first.name.lowercase().compareToWithCollator(b.first.name.lowercase())
                    }
                }
                SetMigrateSorting.Mode.TOTAL -> {
                    when {
                        a.first.isStub && !b.first.isStub -> -1
                        b.first.isStub && !a.first.isStub -> 1
                        else -> a.second.compareTo(b.second)
                    }
                }
            }
        }

        return when (direction) {
            SetMigrateSorting.Direction.ASCENDING -> Comparator(sortFn)
            SetMigrateSorting.Direction.DESCENDING -> Collections.reverseOrder(sortFn)
        }
    }
}
