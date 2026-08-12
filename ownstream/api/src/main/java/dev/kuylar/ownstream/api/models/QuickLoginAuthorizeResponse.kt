package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class QuickLoginAuthorizeResponse(
	val tokenValid: Boolean,
	val signedIn: Boolean,
	val deviceName: String?
)
