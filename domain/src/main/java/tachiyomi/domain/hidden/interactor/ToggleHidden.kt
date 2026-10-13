package tachiyomi.domain.hidden.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.hidden.model.HiddenItemType
import tachiyomi.domain.hidden.repository.HiddenRepository

@Inject
class ToggleHidden(
    private val repository: HiddenRepository,
) {

    suspend fun manga(id: Long, hidden: Boolean) {
        repository.setHidden(HiddenItemType.Manga, id, hidden)
    }

    suspend fun source(id: Long, hidden: Boolean) {
        repository.setHidden(HiddenItemType.Source, id, hidden)
    }
}
