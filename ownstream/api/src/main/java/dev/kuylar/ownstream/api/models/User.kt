package dev.kuylar.ownstream.api.models

data class User(
	val id: String,
	val username: String,
	val permissions: List<String>
)
