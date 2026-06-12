package dev.kuylar.ownstream.api.models

data class DiskUsage(
	val used: Long,
	val total: Long,
	val available: Long
)
