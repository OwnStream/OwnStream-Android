package dev.kuylar.ownstream.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.OwnStreamAccount
import dev.kuylar.ownstream.api.store.OwnStreamServer
import dev.kuylar.ownstream.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
	private lateinit var binding: ActivityLoginBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		binding = ActivityLoginBinding.inflate(layoutInflater)
		setContentView(binding.root)

		ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val sp = getSharedPreferences("main", MODE_PRIVATE)
		sp.getString("host", null)?.let {
			binding.instance.editText?.editableText?.insert(0, it)
		}


		binding.button.setOnClickListener {
			lifecycleScope.launch {
				binding.button.isEnabled = false
				// TODO: Check server
				val input = binding.instance.editText!!.text.toString()
				val server = OwnStreamServer(
					UUID.randomUUID().toString(),
					input,
					input.toUri().authority ?: input
				)
				val existing = client.store.getServers().firstOrNull { it.url == input }
				if (existing == null) {
					client.store.addServer(server)
				}
				client.selectServer((existing ?: server).id)
				try {
					client.getInfo()
				} catch (e: Exception) {
					// not an os server
					withContext(Dispatchers.Main) {
						Toast.makeText(
							this@LoginActivity,
							R.string.login_error_invalid_server,
							Toast.LENGTH_LONG
						).show()
					}
					client.store.removeServer(existing ?: server)
					return@launch
				}
				withContext(Dispatchers.IO) {
					try {
						val loginResp = client.login(
							binding.username.editText!!.text.toString(),
							binding.password.editText!!.text.toString()
						)
						if (!loginResp.response?.success!!)
							throw Exception(loginResp.response?.message)
						val user = client.whoAmI(
							loginResp.response?.accessToken
								?: throw Exception("Failed to get token")
						).response
						if (user == null)
							throw Exception("Failed to get user info")
						val acc = OwnStreamAccount(
							UUID.randomUUID().toString(),
							(existing ?: server).id,
							user.username,
							loginResp.response!!.accessToken!!,
							user.permissions
						)
						client.store.addAccount(acc)
						client.selectAccount(acc.id)
						withContext(Dispatchers.Main) {
							Toast.makeText(
								this@LoginActivity,
								getString(R.string.login_success, user.username),
								Toast.LENGTH_LONG
							).show()
							startActivity(Intent(this@LoginActivity, MainActivity::class.java))
							finish()
						}
					} catch (e: Exception) {
						withContext(Dispatchers.Main) {
							Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
							binding.button.isEnabled = true
						}
						Log.e(this.javaClass.name, "Exception while logging in", e)
					}
				}
			}
		}
	}
}