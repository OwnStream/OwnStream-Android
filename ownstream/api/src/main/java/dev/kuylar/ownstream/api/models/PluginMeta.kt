package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PluginMeta(
	val id: String,
	val name: String,
	val version: PluginVersion,
	val index: String,
	val image: String,
	val permissions: List<String>,
	val features: List<String>,
	val homepage: String?,
	val source: String?,
	val issues: String?,
	val autoUpdater: PluginUpdaterConfig?
) {
	@Serializable
	data class PluginVersion(val name: String, val code: Int)

	@Serializable
	data class PluginUpdaterConfig(val type: String, val params: Map<String, String>)
}
