package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PluginItem(
	val id: String,
	val version: String,
	val path: String,
	val state: String
)
