package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Images(
	val poster: String? = null,
	val banner: String? = null,
	val logo: String? = null,
	val backdrop: String? = null,
	val thumbnail: String? = null
)