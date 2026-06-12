package dev.kuylar.ownstream.api.models

data class Library(
	val id: String,
	val name: String,
	val path: String,
	val diskUsage: DiskUsage
)
