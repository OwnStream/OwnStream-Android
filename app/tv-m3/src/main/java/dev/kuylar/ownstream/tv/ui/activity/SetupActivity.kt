package dev.kuylar.ownstream.tv.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivitySetupBinding
import dev.kuylar.ownstream.tv.ui.fragment.setup.SetupLoginFragment
import dev.kuylar.ownstream.tv.ui.fragment.setup.SetupServerSelectFragment
import dev.kuylar.ownstream.tv.ui.fragment.setup.SetupWelcomeFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
	private lateinit var binding: ActivitySetupBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		binding = ActivitySetupBinding.inflate(layoutInflater)
		setContentView(binding.root)

		lifecycleScope.launch {
			val servers = client.store.getServers()
			if (servers.isEmpty()) openServerFragment()
			else openServerSelectFragment()
		}
	}

	fun openServerFragment() {
		supportFragmentManager.beginTransaction()
			.replace(binding.root.id, SetupWelcomeFragment())
			.commit()
	}

	fun openServerSelectFragment() {
		supportFragmentManager.beginTransaction()
			.replace(binding.root.id, SetupServerSelectFragment())
			.commit()
	}

	fun openLoginFragment() {
		supportFragmentManager.beginTransaction()
			.replace(binding.root.id, SetupLoginFragment())
			.setTransition(TRANSIT_FRAGMENT_FADE)
			.commit()
	}

	fun openManualLoginFragment() {
		supportFragmentManager.beginTransaction()
			.replace(binding.root.id, SetupLoginFragment())
			.setTransition(TRANSIT_FRAGMENT_FADE)
			.commit()
	}

	fun finishSetup() {
		finish()
		startActivity(Intent(this, MainActivity::class.java))
	}
}