package dev.kuylar.ownstream.api.models

data class QuickLoginSession(
	val deviceName: String,
	val code: String,
	val expiresAt: String
)
