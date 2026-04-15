package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Video(
	val id: String,
	val encodingSettings: String,
	val width: Long,
	val height: Long,
	val fps: Long,
	val language: String,
	val previewFiles: List<PreviewFile>? = null,
	val subtitles: List<SubtitleFile>? = null,
	val episode: Episode? = null,
)