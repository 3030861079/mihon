package tachiyomi.domain.hidden

/**
 * An entry belongs to the hidden space if the manga itself was hidden, or it comes from a
 * hidden source (e.g. reading from a hidden source without separately hiding the manga).
 */
fun isHiddenEntry(
    mangaId: Long,
    sourceId: Long,
    hiddenMangaIds: Set<Long>,
    hiddenSourceIds: Set<Long>,
): Boolean {
    return mangaId in hiddenMangaIds || sourceId in hiddenSourceIds
}

/** Whether the entry should appear in the current hidden-space session. */
fun isVisibleInHiddenSpace(
    mangaId: Long,
    sourceId: Long,
    hiddenMangaIds: Set<Long>,
    hiddenSourceIds: Set<Long>,
    inHiddenSpace: Boolean,
): Boolean {
    return isHiddenEntry(mangaId, sourceId, hiddenMangaIds, hiddenSourceIds) == inHiddenSpace
}
