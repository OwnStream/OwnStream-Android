package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class InstanceInfo(
	val type: String,
	val version: String,
	val name: String,
	val setupComplete: Boolean
)
