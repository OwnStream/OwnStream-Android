package dev.kuylar.ownstream.tvleanback.ui.fragment.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.edit
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tvleanback.ui.activity.MainActivity
import dev.kuylar.ownstream.tvleanback.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragmentAccountLogin : GuidedStepSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient
	override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
		return GuidanceStylist.Guidance(
			getString(R.string.setup_account_title),
			getString(R.string.setup_account_description),
			getString(R.string.setup_account_breadcrumb),
			null
		)
	}

	override fun onCreateActions(
		actions: MutableList<GuidedAction?>,
		savedInstanceState: Bundle?
	) {
		super.onCreateActions(actions, savedInstanceState)

		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_account_username.toLong())
			this.title(R.string.setup_account_username)
			this.descriptionEditable(true)
		}.build())
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_account_password.toLong())
			this.title(R.string.setup_account_password)
			this.descriptionEditable(true)
			this.descriptionInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
			this.descriptionEditInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
		}.build())
	}

	override fun onCreateButtonActions(
		actions: MutableList<GuidedAction?>,
		savedInstanceState: Bundle?
	) {
		super.onCreateButtonActions(actions, savedInstanceState)
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_account_continue.toLong())
			this.title(R.string.setup_account_continue)
			this.hasNext(true)
		}.build())
	}

	override fun onGuidedActionClicked(action: GuidedAction) {
		super.onGuidedActionClicked(action)
		val actions = actions.associate { Pair(it.id, it.description?.toString() ?: "") }
		if (action.id == R.id.setup_account_continue.toLong()) {
			Toast.makeText(requireContext(), R.string.logging_in, Toast.LENGTH_SHORT).show()
			lifecycleScope.launch {
				withContext(Dispatchers.IO) {
					runCatching {
						val res = client.login(
							actions[R.id.setup_account_username.toLong()] ?: "",
							actions[R.id.setup_account_password.toLong()] ?: ""
						).response!!

						if (!res.success) throw Exception(res.message)
						val sp = requireContext().getSharedPreferences("main", Context.MODE_PRIVATE)
						sp.edit {
							putString("token", res.accessToken)
						}
						return@runCatching res
					}.onFailure {
						Toast.makeText(requireContext(), getString(R.string.login_fail, it.message), Toast.LENGTH_LONG).show()
					}.onSuccess {
						finishGuidedStepSupportFragments()
						startActivity(Intent(requireContext(), MainActivity::class.java))
					}
				}
			}
		}
	}
}