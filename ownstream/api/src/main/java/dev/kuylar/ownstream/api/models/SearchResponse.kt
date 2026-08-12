package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
@Deprecated("will be replaced with PagedResponse soon")
data class SearchResponse(
	val results: List<SearchResult>,
	val total: Int,
	val hasMore: Boolean
)
