package dev.kuylar.ownstream

import android.view.View
import android.widget.TextView
import java.time.Duration
import java.time.Instant

object Utils {
	fun View.visibleIf(value: Boolean, orElse: Int = View.GONE) {
		this.visibility = if (value) View.VISIBLE else orElse
	}

	fun TextView.visibleIfNotBlank() {
		this.visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
	}

	fun <T> Map<String, T>.firstOf(vararg keys: String): T? {
		keys.forEach {
			if (containsKey(it)) return get(it)
		}
		return null
	}

	fun toHhMmSsRelativeToNow(from: String): String =
		toHhMmSsRelativeTo(Instant.parse(from), Instant.now())

	fun toHhMmSsRelativeTo(from: String, to: String): String =
		toHhMmSsRelativeTo(Instant.parse(from), Instant.parse(to))

	fun toHhMmSsRelativeTo(from: Instant, to: Instant): String {
		val duration = Duration.between(from, to)

		val hours = duration.toHours()
		val minutes = duration.toMinutes() % 60
		val seconds = duration.seconds % 60

		return when {
			hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
			minutes > 0 -> "${minutes}m ${seconds}s"
			else -> "${seconds}s"
		}
	}
}