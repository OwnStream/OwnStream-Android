package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
	val id: String,
	val seasonNumber: Int,
	val episodeNumber: Int,
	val originalTitle: String,
	val translatedTitle: String? = null,
	val originalSummary: String,
	val translatedSummary: String? = null,
	val thumbnail: String?,
	val createdAt: String,
	val updatedAt: String,
	val releasedAt: String,
	val videos: List<Video>,
	val runtime: String?,
	val progress: Float?
)
