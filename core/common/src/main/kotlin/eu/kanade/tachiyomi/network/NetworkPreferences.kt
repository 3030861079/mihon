package eu.kanade.tachiyomi.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.core.metro.IsDebugBuild
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@Inject
@SingleIn(AppScope::class)
class NetworkPreferences(
    preferenceStore: PreferenceStore,
    @IsDebugBuild isDebugBuild: Boolean,
) {

    val verboseLogging: Preference<Boolean> = preferenceStore.getBoolean(
        "verbose_logging",
        isDebugBuild,
    )

    val dohProvider: Preference<Int> = preferenceStore.getInt("doh_provider", -1)

    val defaultUserAgent: Preference<String> = preferenceStore.getString(
        "default_user_agent",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36",
    )

    /**
     * Sends a `Referer` header on image requests that don't already carry one.
     *
     * Extensions are only required to send a user agent, so image hosts that enforce hotlink
     * protection answer with 403. Enabled by default because it fixes those sources without any
     * downside in practice — the header is only added when the extension didn't set one itself,
     * and it's a standard header that plain browsers send anyway.
     */
    val imageRefererEnabled: Preference<Boolean> = preferenceStore.getBoolean(
        "image_referer_enabled",
        true,
    )

    /**
     * Overrides the `Referer` sent by [imageRefererEnabled]. When blank, the referer is derived
     * from the image URL itself (its scheme and host).
     */
    val imageRefererValue: Preference<String> = preferenceStore.getString(
        "image_referer_value",
        "",
    )
}
