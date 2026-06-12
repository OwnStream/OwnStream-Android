package dev.kuylar.ownstream.api.models

data class QuickLoginAuthorizeResponse(
	val tokenValid: Boolean,
	val signedIn: Boolean,
	val deviceName: String?
)
