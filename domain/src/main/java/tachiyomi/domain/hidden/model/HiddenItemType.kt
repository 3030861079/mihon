package tachiyomi.domain.hidden.model

/** Kinds of entries that can be moved into the hidden space. */
enum class HiddenItemType(val value: Long) {
    Manga(0L),
    Source(1L),
}
