package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeMetadata(
	val id: String,
	val season: Int,
	val episode: Int,
	val title: String,
	val translatedTitle: Map<String, String>,
	val summary: String,
	val translatedSummary: Map<String, String>,
	val thumbnail: String?,
	val createdAt: String,
	val updatedAt: String,
	val releasedAt: String,
)
