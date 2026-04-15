package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Content(
	val id: String,
	val type: String,
	val originalTitle: String,
	val translatedTitle: String? = null,
	val originalTagline: String,
	val translatedTagline: String? = null,
	val originalDescription: String,
	val translatedDescription: String? = null,
	val images: Images,
	val createdAt: String,
	val updatedAt: String,
	val releasedAt: String,
	val lastAiredAt: String?,
	val seasonCount: Int?,
	val episodeCount: Int?,
	val videoCount: Int?,
	val ageRatings: Map<String, String>,
	val externalIds: Map<String, String>,
)