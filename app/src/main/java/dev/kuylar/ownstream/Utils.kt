package dev.kuylar.ownstream

import android.view.View
import android.widget.TextView

object Utils {
	fun View.visibleIf(value: Boolean) {
		this.visibility = if (value) View.VISIBLE else View.GONE
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
}