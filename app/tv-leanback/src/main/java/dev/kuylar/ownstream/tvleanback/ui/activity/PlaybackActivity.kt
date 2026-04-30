package dev.kuylar.ownstream.tvleanback.ui.activity

import android.R
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.tvleanback.ui.fragment.PlaybackVideoFragment

@AndroidEntryPoint
class PlaybackActivity : FragmentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.content, PlaybackVideoFragment())
				.commit()
		}
	}
}