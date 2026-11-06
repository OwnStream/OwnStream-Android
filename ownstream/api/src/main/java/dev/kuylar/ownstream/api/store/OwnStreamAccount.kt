package dev.kuylar.ownstream.api.store

import kotlinx.serialization.Serializable

@Serializable
data class OwnStreamAccount(
	val id: String,
	val serverId: String,
	val username: String,
	val token: String,
	val permissions: List<String>
)
