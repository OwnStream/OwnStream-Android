package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class QuickLoginSession(
	val deviceName: String,
	val code: String,
	val expiresAt: String
)
