package dev.kuylar.ownstream.api.models

data class QuickLoginStartResponse(
	val deviceName: String,
	val token: String,
	val code: String
)
