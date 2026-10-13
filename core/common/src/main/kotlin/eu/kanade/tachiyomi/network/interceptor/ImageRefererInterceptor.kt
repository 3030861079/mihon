package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds a `Referer` header to image requests that don't already carry one.
 *
 * Extensions are only obligated to send a user agent ([Request.header] `User-Agent`), so image hosts
 * behind hotlink protection — which reject requests with a missing or foreign referer — answer with
 * 403. This is common on sources whose chapters are served from more than one image host, where only
 * some chapters break.
 *
 * The referer defaults to the image URL's own origin, which satisfies the widespread
 * `valid_referers ... server_names` rule. [customReferer] overrides it for hosts that instead expect
 * the reader site's domain.
 *
 * Only applies to GET requests that look like images and is a no-op when the request already sets
 * its own referer, so extensions that handle this correctly are left untouched.
 */
class ImageRefererInterceptor(
    private val enabled: () -> Boolean,
    private val customReferer: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!enabled() || !request.isRefererApplicable()) {
            return chain.proceed(request)
        }

        val referer = customReferer().trim().ifEmpty { request.url.originUrl() }

        return chain.proceed(
            request.newBuilder()
                .header("Referer", referer)
                .build(),
        )
    }
}

private fun Request.isRefererApplicable(): Boolean {
    if (method != "GET") return false
    if (!header("Referer").isNullOrEmpty()) return false

    val lastSegment = url.pathSegments.lastOrNull().orEmpty()
    return IMAGE_EXTENSION.containsMatchIn(lastSegment) ||
        header("Accept")?.contains("image/") == true
}

/** `https://host/path/file.jpg?q=1` becomes `https://host/`. */
private fun HttpUrl.originUrl(): String {
    return newBuilder()
        .encodedPath("/")
        .query(null)
        .fragment(null)
        .build()
        .toString()
}

private val IMAGE_EXTENSION = Regex(
    pattern = "\\.(jpe?g|png|webp|gif|bmp|avif|heic|heif|jxl|tiff?)$",
    option = RegexOption.IGNORE_CASE,
)
