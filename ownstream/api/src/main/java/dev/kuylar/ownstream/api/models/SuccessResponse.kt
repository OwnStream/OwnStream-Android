package dev.kuylar.ownstream.api.models

import kotlinx.serialization.Serializable

@Serializable
sealed class SuccessResponse(
	val success: Boolean,
	val message: String? = null,
) {
	class WithData<T>(
		success: Boolean,
		message: String? = null,
		val data: T? = null
	): SuccessResponse(success, message)
}

