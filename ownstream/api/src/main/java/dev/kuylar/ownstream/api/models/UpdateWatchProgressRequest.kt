package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateWatchProgressRequest(
	val videoId: String,
	val videoLength: Int?,
	val watchedMilliseconds: Int?,
	val markAsWatched: Boolean?
)