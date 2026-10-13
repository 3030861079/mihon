package tachiyomi.domain.hidden.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.hidden.repository.HiddenRepository

@Inject
class GetHiddenIds(
    private val repository: HiddenRepository,
) {

    fun subscribe(type: HiddenItemType): Flow<Set<Long>> {
        return repository.getIds(type)
    }

    suspend fun await(type: HiddenItemType): Set<Long> {
        return repository.getIdsAwait(type)
    }
}
