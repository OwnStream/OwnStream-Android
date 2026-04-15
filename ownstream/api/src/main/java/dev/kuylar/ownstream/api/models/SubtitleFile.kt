package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleFile (
	val id: Int,
	val files: Map<String, String>,
	val default: Boolean,
	val forced: Boolean,
	val language: String,
	val title: String,
)