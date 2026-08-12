package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
data class InputLibrary(
	val id: String,
	val name: String,
	val path: String,
	val type: String,
	val transcodeLibraryId: String
)
