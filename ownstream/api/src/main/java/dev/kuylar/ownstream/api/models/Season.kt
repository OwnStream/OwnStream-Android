package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Season(
	val index: Int,
	val episodeCount: Int,
)
