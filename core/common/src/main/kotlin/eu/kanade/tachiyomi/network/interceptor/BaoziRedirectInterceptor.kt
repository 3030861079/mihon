package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * The Baozimh family of sites (baozimh.com, webmota.com, kukuc.co, twmanga.com, dinnerku.com and
 * their mirrors) serves chapter links as `/user/page_direct?...`, which 302-redirects to a
 * dedicated delivery domain (`baozimhcn.com`, `twbzmg.com`) that refuses or simply times out for
 * direct app traffic. The very same mirrors serve the standard `/comic/chapter/...` page without
 * any fuss, so when one of these mirrors redirects us, pin the redirect target back onto the
 * domain we were already talking to and follow it ourselves.
 */
class BaoziRedirectInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (BAOZI_HOST_SUFFIXES.none { request.url.host.endsWith(it, ignoreCase = true) }) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)
        if (!response.isRedirect) return response

        val location = response.header("Location") ?: return response
        val locationUrl = location.toHttpUrlOrNull() ?: return response
        if (locationUrl.host == request.url.host) return response

        // Same path, same query - only the host (and scheme) are pinned back to the mirror we know.
        val pinned = locationUrl.newBuilder()
            .scheme(request.url.scheme)
            .host(request.url.host)
            .build()

        response.close()
        request = request.newBuilder().url(pinned).build()
        return chain.proceed(request)
    }

    private companion object {
        val BAOZI_HOST_SUFFIXES = listOf(
            "baozimh.com",
            "baozimhcn.com",
            "twbzmg.com",
            "webmota.com",
            "kukuc.co",
            "twmanga.com",
            "dinnerku.com",
        )
    }
}
