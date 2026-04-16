package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeToWatchResponse(
	val continueWatching: Episode?,
	val progress: Float?,
	val upNext: Episode?,
)