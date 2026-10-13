package tachiyomi.data.hidden

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.hidden.repository.HiddenRepository
import kotlin.time.Clock

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class HiddenRepositoryImpl(
    private val database: Database,
) : HiddenRepository {

    override fun getIds(type: HiddenItemType): Flow<Set<Long>> {
        return database.hidden_itemQueries
            .getIdsByType(type.value)
            .subscribeToList()
            .map { it.toSet() }
    }

    override suspend fun getIdsAwait(type: HiddenItemType): Set<Long> {
        return database.hidden_itemQueries
            .getIdsByType(type.value)
            .awaitAsList()
            .toSet()
    }

    override suspend fun setHidden(type: HiddenItemType, itemId: Long, hidden: Boolean) {
        if (hidden) {
            database.hidden_itemQueries.insert(
                type = type.value,
                itemId = itemId,
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )
        } else {
            database.hidden_itemQueries.delete(type = type.value, itemId = itemId)
        }
    }
}
