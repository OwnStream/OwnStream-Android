package dev.kuylar.ownstream.tv.ui.fragment.setup

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.OwnStreamAccount
import dev.kuylar.ownstream.api.store.OwnStreamServer
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.FragmentSetupLoginBinding
import dev.kuylar.ownstream.tv.ui.activity.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SetupLoginFragment : Fragment() {
	private lateinit var binding: FragmentSetupLoginBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentSetupLoginBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		lifecycleScope.launch(Dispatchers.IO) {
			var retrying = true
			while (retrying) {
				val start = client.quickLoginStart("OwnStream on Android TV")
				if (start.response == null) {
					(activity as? SetupActivity)?.openManualLoginFragment()
					break
				}
				withContext(Dispatchers.Main) {
					binding.code.text = start.response!!.code.chunked(3).joinToString(" ")
				}
				while (true) {
					sleep(5000)
					val check = client.quickLoginCheck(start.response!!.token).response
					if (check == null) {
						break // get new code, retry
					} else if (check.loginComplete) {
						Log.i("QuickLogin", "Got token: ${check.signInResult}")
						val userInfo = runCatching {
							client.whoAmI(check.signInResult).response
						}.getOrElse { null }
						if (userInfo == null) {
							lifecycleScope.launch(Dispatchers.Main) {
								Toast.makeText(
									requireContext(),
									R.string.error_quick_login_user_info_failed,
									Toast.LENGTH_LONG
								).show()
							}
							break
						}
						val account = OwnStreamAccount(
							"${client.serverId}/${userInfo.id}",
							client.serverId!!,
							userInfo.username,
							check.signInResult!!,
							userInfo.permissions
						)
						client.store.addAccount(account)
						client.selectAccount(account.id)

						lifecycleScope.launch(Dispatchers.Main) {
							(activity as? SetupActivity)?.finishSetup()
						}
						retrying = false
					}
				}
			}
		}
	}
}