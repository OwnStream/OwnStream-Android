package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Job(
	val id: String,
	val jobType: String,
	val status: String,
	val message: String?,
	val progress: Int?,
	val progressMax: Int?,
	val createdAt: String,
	val startedAt: String?,
	val updatedAt: String?,
	val completedAt: String?,
	val relevantVideoId: String?,
	val relevantEpisodeId: String?,
	val relevantContentId: String?,
	val relevantLibraryId: String?,
	val relevantWebhookId: String?,
)
