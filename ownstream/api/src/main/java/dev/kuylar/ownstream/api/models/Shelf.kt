package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Shelf(
	val title: String,
	val type: String,
	val description: String? = null,
	val icon: String? = null,
	val items: List<ShelfItem>,
)