package tachiyomi.domain.hidden.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.hidden.model.HiddenItemType

interface HiddenRepository {

    fun getIds(type: HiddenItemType): Flow<Set<Long>>

    suspend fun getIdsAwait(type: HiddenItemType): Set<Long>

    suspend fun setHidden(type: HiddenItemType, itemId: Long, hidden: Boolean)
}
