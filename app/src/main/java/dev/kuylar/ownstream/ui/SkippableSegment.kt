package dev.kuylar.ownstream.ui

import com.github.vkay94.timebar.YouTubeSegment
import dev.kuylar.ownstream.api.models.VideoSegment

class SkippableSegment(
	override var color: Int,
	override val endTimeMs: Long,
	override val startTimeMs: Long,
	val sourceSegment: VideoSegment
) : YouTubeSegment {
	companion object {
		fun fromSegment(segment: VideoSegment) = SkippableSegment(
			color = segment.type.toColor(),
			endTimeMs = segment.endMilliseconds.toLong(),
			startTimeMs = segment.startMilliseconds.toLong(),
			sourceSegment = segment
		)

		private fun String.toColor() = when (lowercase()) {
			"opening" -> 0x00FFFF
			"ending" -> 0x0202ED
			else -> 0xFF0000
		}
	}
}

