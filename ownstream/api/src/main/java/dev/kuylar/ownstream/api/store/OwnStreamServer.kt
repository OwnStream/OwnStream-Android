package dev.kuylar.ownstream.api.store

import kotlinx.serialization.Serializable

@Serializable
data class OwnStreamServer(
	val id: String,
	val url: String,
	val name: String
)
