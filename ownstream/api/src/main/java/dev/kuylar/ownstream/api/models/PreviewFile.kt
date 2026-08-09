package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PreviewFile (
	val template: String,
	val frameCount: Int,
	val rows: Int,
	val columns: Int,
	val period: Float? = null,
)