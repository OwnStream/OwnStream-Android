package dev.kuylar.ownstream.tvleanback

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.tvleanback.setup.SetupFragmentInstanceUri

@AndroidEntryPoint
class SetupActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_setup)

		GuidedStepSupportFragment.addAsRoot(
			this,
			SetupFragmentInstanceUri(),
			R.id.main_setup_fragment
		)
	}
}