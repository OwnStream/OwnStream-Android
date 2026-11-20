package dev.kuylar.ownstream.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.jvm.java

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

		lifecycleScope.launch(Dispatchers.IO) {
			// TODO: Currently we only use the first server, with no
			//  selection or setting a default one
			val servers = client.store.getServers()
			val serverId = servers.firstOrNull()?.id
			val accounts = client.store.getAccounts(serverId ?: "")
			val accountId = accounts.firstOrNull()?.id
			if (serverId != null && accountId != null) {
				client.selectServer(serverId)
				client.selectAccount(accountId)
				withContext(Dispatchers.Main) {
					loadData()
				}
			} else {
				withContext(Dispatchers.Main) {
					startActivity(Intent(this@MainActivity, LoginActivity::class.java))
					finish()
				}
			}
		}
	}

	private fun loadData() {
		val navHostFragment =
			supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
		val navController = navHostFragment.navController
		binding.toolbar.setupWithNavController(navController)
		binding.bottomNavigation.setupWithNavController(navController)
		binding.navigationRail.setupWithNavController(navController)

		setupNavigation()
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		setupNavigation()
	}

	private fun setupNavigation() {
		val screenWidthDp = resources.configuration.screenWidthDp
		val useNavigationRail = screenWidthDp >= 600

		if (useNavigationRail) {
			binding.navigationRail.visibility = View.VISIBLE
			binding.bottomNavigation.visibility = View.GONE
		} else {
			binding.navigationRail.visibility = View.GONE
			binding.bottomNavigation.visibility = View.VISIBLE
		}
	}
}