package dev.kuylar.ownstream.api

data class ApiResponse<T>(
	val responseCode: Int,
	val response: T?
)