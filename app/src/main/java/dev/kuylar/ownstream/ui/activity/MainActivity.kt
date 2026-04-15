package dev.kuylar.ownstream.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.databinding.ActivityMainBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		if (client.instanceHost == "https://invalid") {
			startActivity(Intent(this, LoginActivity::class.java))
			finish()
			return
		}

		val navHostFragment =
			supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
		val navController = navHostFragment.navController
		binding.topAppBar.setupWithNavController(navController)
		binding.bottomNavigation.setupWithNavController(navController)

		binding.bottomNavigation.post {
			binding.navHostFragment.setPadding(
				binding.navHostFragment.paddingLeft,
				binding.navHostFragment.paddingTop,
				binding.navHostFragment.paddingRight,
				binding.bottomNavigation.height
			)
		}
	}
}