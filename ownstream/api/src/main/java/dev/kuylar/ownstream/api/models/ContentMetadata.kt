package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ContentMetadata(
	val id: String,
	val type: Int,
	val title: String,
	val translatedTitle: Map<String, String>,
	val tagline: String,
	val translatedTagline: Map<String, String>,
	val description: String,
	val translatedDescription: Map<String, String>,
	val poster: String?,
	val banner: String?,
	val logo: String?,
	val backdrop: String?,
	val thumbnail: String?,
	val createdAt: String,
	val updatedAt: String,
	val releasedAt: String,
	val finishedStreamingAt: String?,
	val ageRatings: Map<String, String>,
	val externalIds: Map<String, String>,
)
