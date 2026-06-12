package dev.kuylar.ownstream.api.models

data class SearchResult(
	val kind: String,
	val id: String,
	val contentId: String?,
	val contentTitle: String?,
	val title: String,
	val description: String?,
	val translatedTitle: String?,
	val translatedDescription: String?,
	val images: Images,
	val season: Int?,
	val episode: Int?,
)
