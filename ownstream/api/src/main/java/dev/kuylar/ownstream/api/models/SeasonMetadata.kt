package dev.kuylar.ownstream.api.models

data class SeasonMetadata(
	val contentId: String,
	val index: Int,
	val title: String,
	val translatedTitle: Map<String, String>,
	val description: String,
	val translatedDescription: Map<String, String>,
	val poster: String?
)
