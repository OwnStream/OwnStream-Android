package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
	val success: Boolean,
	val message: String? = null,
	val accessToken: String? = null,
	val username: String? = null
)
