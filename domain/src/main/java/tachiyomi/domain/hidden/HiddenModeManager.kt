package tachiyomi.domain.hidden

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app is currently showing the hidden space.
 *
 * Deliberately in memory only: killing the app leaves the hidden space, so a stray session does not
 * silently stay reachable.
 */
@Inject
@SingleIn(AppScope::class)
class HiddenModeManager {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    fun enter() {
        _active.value = true
    }

    fun exit() {
        _active.value = false
    }

    fun toggle() {
        _active.value = !_active.value
    }
}
