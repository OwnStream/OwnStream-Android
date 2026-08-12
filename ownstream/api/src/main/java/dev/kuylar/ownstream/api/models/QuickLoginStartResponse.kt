package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class QuickLoginStartResponse(
	val deviceName: String,
	val token: String,
	val code: String
)
