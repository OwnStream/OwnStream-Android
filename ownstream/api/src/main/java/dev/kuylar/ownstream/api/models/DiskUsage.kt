package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class DiskUsage(
	val used: Long,
	val total: Long,
	val available: Long
)
