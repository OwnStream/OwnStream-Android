package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VideoSegment(
	val id: String,
	val type: String,
	val startMilliseconds: Int,
	val endMilliseconds: Int,
	val videoDuration: Int
)
