package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Season(
	val index: Int,
	val episodeCount: Int,
	val title: String?,
	val translatedTitle: String?,
	val description: String?,
	val translatedDescription: String?,
	val poster: String?
)
