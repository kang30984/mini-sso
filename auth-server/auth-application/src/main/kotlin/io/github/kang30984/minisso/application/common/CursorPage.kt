package io.github.kang30984.minisso.application.common

/** No-Offset 페이징 결과. nextCursor가 null이면 마지막 페이지다. */
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: Long?,
) {
    val hasNext: Boolean get() = nextCursor != null
}
