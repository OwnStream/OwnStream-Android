package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Library(
	val id: String,
	val name: String,
	val path: String,
	val diskUsage: DiskUsage?
)
