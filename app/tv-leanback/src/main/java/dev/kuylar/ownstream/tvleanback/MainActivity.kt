package dev.kuylar.ownstream.tvleanback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
	@Inject
	lateinit var client: OwnStreamApiClient
	val mErrorFragment = ErrorFragment()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val sp = getSharedPreferences("main", MODE_PRIVATE)
		if (!sp.contains("host") || !sp.contains("token")) {
			startActivity(Intent(this, SetupActivity::class.java))
			finish()
			return
		}

		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.main_browse_fragment, MainFragment())
				.commitNow()
		}
	}

	fun onError(it: Throwable) {
		Log.e("MainActivity", "onError triggered!", it)
		supportFragmentManager
			.beginTransaction()
			.add(R.id.main_browse_fragment, mErrorFragment)
			.commit()
		lifecycleScope.launch {
			delay(50)
			mErrorFragment.setErrorContent(it)
		}
	}
}