package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
	val items: List<T>,
	val hasMore: Boolean,
	val count: Int,
	val pages: Int
)
