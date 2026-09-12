package dev.kuylar.ownstream.tvleanback.ui.fragment.setup

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tvleanback.BuildConfig
import dev.kuylar.ownstream.tvleanback.R
import dev.kuylar.ownstream.tvleanback.ui.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class SetupFragmentQuickLogin : GuidedStepSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient
	lateinit var token: String
	lateinit var code: String
	private var pollingJob: Job? = null

	override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
		return GuidanceStylist.Guidance(
			getString(R.string.setup_quick_login_title, if (::code.isInitialized) code else "..."),
			getString(R.string.setup_quick_login_description),
			getString(R.string.setup_quick_login_breadcrumb),
			null
		)
	}

	override fun onCreateActions(
		actions: MutableList<GuidedAction?>,
		savedInstanceState: Bundle?
	) {
		super.onCreateActions(actions, savedInstanceState)
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_quick_login_back.toLong())
			this.title(R.string.setup_quick_login_back)
		}.build())
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			withContext(Dispatchers.IO) {
				val fetchedCode = getCode()
				if (fetchedCode != null) {
					code = fetchedCode
					withContext(Dispatchers.Main) {
						if (guidanceStylist.titleView == null) {
							Toast.makeText(
								requireContext(),
								"titleView is null, code is $code",
								Toast.LENGTH_LONG
							).show()
						} else {
							guidanceStylist.titleView?.text =
								getString(R.string.setup_quick_login_title, code)
						}
					}
					startPolling()
				} else {
					withContext(Dispatchers.Main) {
						Toast.makeText(requireContext(), R.string.login_fail, Toast.LENGTH_LONG)
							.show()
					}
				}
			}
		}
	}

	private fun startPolling() {
		pollingJob = lifecycleScope.launch(Dispatchers.IO) {
			while (isActive) {
				val loginComplete = checkCode()
				if (loginComplete) {
					withContext(Dispatchers.Main) {
						finishGuidedStepSupportFragments()
						startActivity(Intent(requireContext(), MainActivity::class.java))
					}
					break
				}
				delay(5.seconds)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		pollingJob?.cancel()
	}

	override fun onGuidedActionClicked(action: GuidedAction) {
		super.onGuidedActionClicked(action)
		val fm = fragmentManager ?: return
		when (action.id.toInt()) {
			R.id.setup_quick_login_back -> {
				pollingJob?.cancel()
				fm.popBackStack()
			}

			else -> {}
		}
	}

	suspend fun getCode(): String? {
		val quickLogin = client.quickLoginStart("OwnStream on Android TV").response ?: return null
		token = quickLogin.token
		return quickLogin.code
	}

	suspend fun checkCode(): Boolean {
		val resp = client.quickLoginCheck(token).response ?: return false
		if (resp.loginComplete) {
			val sp = requireContext().getSharedPreferences("main", MODE_PRIVATE)
			sp.edit {
				putString("token", resp.signInResult)
			}
			client.setAuth(resp.signInResult!!)
			return true
		}
		return false
	}
}