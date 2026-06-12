package dev.kuylar.ownstream.api.models

data class PagedResponse<T>(
	val items: List<T>,
	val hasMore: Boolean,
	val count: Int,
	val pages: Int
)
