package dev.kuylar.ownstream.tvleanback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
	@Inject
	lateinit var client: OwnStreamApiClient

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
			getSupportFragmentManager().beginTransaction()
				.replace(R.id.main_browse_fragment, MainFragment())
				.commitNow()
		}
	}
}