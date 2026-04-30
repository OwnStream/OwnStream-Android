package dev.kuylar.ownstream.tvleanback.ui.fragment

import android.os.Bundle
import android.view.View

import androidx.core.content.ContextCompat
import androidx.leanback.app.ErrorSupportFragment
import dev.kuylar.ownstream.tvleanback.R

class ErrorFragment : ErrorSupportFragment() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		title = resources.getString(R.string.app_name)
	}

	internal fun setErrorContent(it: Throwable? = null) {
		imageDrawable = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_error)
		message = resources.getString(R.string.error_fragment_message)
		if (it != null) {
			message = message.toString() + "\n" + it.message
		}
		setDefaultBackground(TRANSLUCENT)

		buttonText = resources.getString(R.string.dismiss_error)
		buttonClickListener = View.OnClickListener {
			requireFragmentManager().beginTransaction().remove(this@ErrorFragment).commit()
		}
	}

	companion object {
		private val TRANSLUCENT = true
	}
}