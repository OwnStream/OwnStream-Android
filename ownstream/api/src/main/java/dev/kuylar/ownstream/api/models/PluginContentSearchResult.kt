package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PluginContentSearchResult(
	val pluginId: String,
	val externalId: String,
	val titles: List<String>,
	val poster: String? = null,
	val year: Int? = null
)