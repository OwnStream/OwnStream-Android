package dev.kuylar.ownstream.api.models

data class InputLibrary(
	val id: String,
	val name: String,
	val path: String,
	val type: String,
	val transcodeLibraryId: String
)
