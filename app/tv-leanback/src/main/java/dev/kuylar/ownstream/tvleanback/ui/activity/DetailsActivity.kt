package dev.kuylar.ownstream.tvleanback.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.tvleanback.R
import dev.kuylar.ownstream.tvleanback.ui.fragment.ErrorFragment
import dev.kuylar.ownstream.tvleanback.ui.fragment.VideoDetailsFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailsActivity : FragmentActivity() {
	val mErrorFragment = ErrorFragment()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_details)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.details_fragment, VideoDetailsFragment())
				.commitNow()
		}
	}

	fun onError(it: Throwable) {
		Log.e("DetailsActivity", "onError triggered!", it)
		supportFragmentManager
			.beginTransaction()
			.add(R.id.main_browse_fragment, mErrorFragment)
			.commit()
		lifecycleScope.launch {
			delay(50)
			mErrorFragment.setErrorContent(it)
		}
	}

	companion object {
		const val SHARED_ELEMENT_NAME = "hero"
		const val MOVIE = "Movie"
	}
}