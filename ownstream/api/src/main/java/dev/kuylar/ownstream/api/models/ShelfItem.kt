package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ShelfItem(
	val type: String,
	val id: String,
	val episodeId: String? = null,
	val videoId: String? = null,
	val subtitle: List<String>,
	val title: String,
	val image: String,
)