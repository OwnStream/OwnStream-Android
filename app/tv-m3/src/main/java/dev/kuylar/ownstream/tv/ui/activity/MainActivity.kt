package dev.kuylar.ownstream.tv.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivityMainBinding
import dev.kuylar.ownstream.tv.ui.fragment.home.HomeFragment
import dev.kuylar.ownstream.tv.ui.fragment.home.LibraryFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding

	@Inject
	lateinit var api: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		if (intent?.action == Intent.ACTION_VIEW) {
			val uri = intent.data?.toString()?.substringAfter("/video/")
			val type = uri?.split("/")[0]
			val id = uri?.split("/")[1]
			Toast.makeText(this, "Type: $type | ID: $id", Toast.LENGTH_LONG).show()
		}

		lifecycleScope.launch(Dispatchers.IO) {
			// TODO: Currently we only use the first server, with no
			//  selection or setting a default one
			val servers = api.store.getServers()
			val serverId = servers.firstOrNull()?.id
			val accounts = api.store.getAccounts(serverId ?: "")
			val accountId = accounts.firstOrNull()?.id
			if (serverId != null && accountId != null) {
				api.selectServer(serverId)
				api.selectAccount(accountId)
				withContext(Dispatchers.Main) {
					loadData()
				}
			} else {
				withContext(Dispatchers.Main) {
					startActivity(Intent(this@MainActivity, SetupActivity::class.java))
					finish()
				}
			}
		}
	}

	private fun loadData() {
		loadFragment(0)
		binding.tabsRow.setOnTabSelectedListener { index, tab ->
			loadFragment(index)
		}
	}

	private val fragments = mutableMapOf<Int, Fragment>()
	private fun loadFragment(index: Int) {
		if (!fragments.containsKey(index)) {
			fragments[index] = getFragment(index)
		}
		supportFragmentManager.beginTransaction()
			.replace(binding.fragment.id, fragments[index]!!)
			.commit()
	}

	private fun getFragment(index: Int) = when (index) {
		/* Home */ 0 -> HomeFragment()
		/* Library/Movies */ 1 -> LibraryFragment().withArg("type", "Movie")
		/* Library/TV */ 2 -> LibraryFragment().withArg("type", "Tv")
		else -> throw IllegalArgumentException("Invalid tab index")
	}

	private fun Fragment.withArg(key: String, value: String): Fragment {
		val args = arguments ?: Bundle()
		args.putString(key, value)
		arguments = args
		return this
	}
}