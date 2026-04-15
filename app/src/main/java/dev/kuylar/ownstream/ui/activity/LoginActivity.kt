package dev.kuylar.ownstream.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.BuildConfig
import dev.kuylar.ownstream.ui.activity.MainActivity
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.ApiResponse
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
	private lateinit var binding: ActivityLoginBinding

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
				val client = OwnStreamApiClient(
					binding.instance.editText!!.text.toString(),
					"OwnStream-Android/${BuildConfig.VERSION_NAME}"
				)
				val resp = withContext(Dispatchers.IO) {
					try {
						client.login(
							binding.username.editText!!.text.toString(),
							binding.password.editText!!.text.toString()
						)
					} catch (e: Exception) {
						Log.e(this.javaClass.name, "Exception while logging in", e)
						ApiResponse(0, null)
					}
				}.response

				if (resp?.success == true) {
					Toast.makeText(
						this@LoginActivity,
						getString(R.string.login_success, resp.username),
						Toast.LENGTH_LONG
					).show()
					val sp = getSharedPreferences("main", MODE_PRIVATE)
					sp.edit {
						putString("host", binding.instance.editText!!.text.toString())
						putString("token", resp.accessToken)
					}
					startActivity(Intent(this@LoginActivity, MainActivity::class.java))
					finish()
				} else {
					Toast.makeText(
						this@LoginActivity,
						resp?.message ?: getString(R.string.login_error_null_response),
						Toast.LENGTH_LONG
					).show()
					binding.button.isEnabled = true
				}
			}
		}
	}
}