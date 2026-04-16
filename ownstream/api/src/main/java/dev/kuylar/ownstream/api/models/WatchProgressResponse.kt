package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class WatchProgressResponse(
	val videoId: String,
	val position: Int?,
	val duration: Int?,
	val wasMarkedAsWatched: Boolean?
)