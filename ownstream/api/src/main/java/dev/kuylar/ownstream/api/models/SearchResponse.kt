package dev.kuylar.ownstream.api.models

@Deprecated("will be replaced with PagedResponse soon")
data class SearchResponse(
	val results: List<SearchResult>,
	val total: Int,
	val hasMore: Boolean
)
