package dev.kuylar.ownstream.api.models

data class QuickLoginCheckResponse(
	val tokenValid: Boolean,
	val loginComplete: Boolean,
	val signInResult: String?
)
