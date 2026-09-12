package dev.kuylar.ownstream.tvleanback.ui.fragment.setup

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tvleanback.BuildConfig
import dev.kuylar.ownstream.tvleanback.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupFragmentLoginType : GuidedStepSupportFragment() {
	override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
		return GuidanceStylist.Guidance(
			getString(R.string.setup_login_type_title),
			getString(R.string.setup_login_type_description),
			getString(R.string.setup_login_type_breadcrumb),
			null
		)
	}

	override fun onCreateActions(
		actions: MutableList<GuidedAction?>,
		savedInstanceState: Bundle?
	) {
		super.onCreateActions(actions, savedInstanceState)
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_login_type_quick.toLong())
			this.title(R.string.setup_login_type_quick)
		}.build())
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_login_type_password.toLong())
			this.title(R.string.setup_login_type_password)
		}.build())
	}

	override fun onGuidedActionClicked(action: GuidedAction) {
		super.onGuidedActionClicked(action)
		val fm = fragmentManager ?: return
		when (action.id.toInt()) {
			R.id.setup_login_type_password -> add(fm, SetupFragmentAccountLogin())
			R.id.setup_login_type_quick -> add(fm, SetupFragmentQuickLogin())
			else -> {}
		}
	}
}