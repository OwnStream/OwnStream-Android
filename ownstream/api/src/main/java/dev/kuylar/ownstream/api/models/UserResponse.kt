package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
	val id: String,
	val username: String,
	val permissions: List<String>
)
