package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class PreviewFile (
	val template: String,
	val frameCount: Long,
	val rows: Long,
	val columns: Long,
	val period: Long? = null,
)