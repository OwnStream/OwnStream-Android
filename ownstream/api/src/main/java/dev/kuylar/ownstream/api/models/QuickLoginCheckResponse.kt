package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class QuickLoginCheckResponse(
	val tokenValid: Boolean,
	val loginComplete: Boolean,
	val signInResult: String?
)
