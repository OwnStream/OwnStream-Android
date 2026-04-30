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

class SetupFragmentInstanceUri : GuidedStepSupportFragment() {
	override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
		return GuidanceStylist.Guidance(
			getString(R.string.setup_instance_title),
			getString(R.string.setup_instance_description),
			getString(R.string.setup_instance_breadcrumb),
			null
		)
	}

	override fun onCreateActions(
		actions: MutableList<GuidedAction?>,
		savedInstanceState: Bundle?
	) {
		super.onCreateActions(actions, savedInstanceState)
		val sp = requireContext().getSharedPreferences("main", MODE_PRIVATE)
		actions.add(GuidedAction.Builder(requireContext()).apply {
			this.id(R.id.setup_instance_uri.toLong())
			this.title(R.string.setup_instance_uri)
			this.descriptionEditable(true)
			this.editDescription(sp.getString("host", "https://"))
			this.descriptionEditInputType(InputType.TYPE_TEXT_VARIATION_URI)
		}.build())
	}

	override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
		Log.i(
			"SetupFragmentInstanceUri",
			"onGuidedActionEditedAndProceed: ${action.editDescription}"
		)
		val fm = fragmentManager ?: return GuidedAction.ACTION_ID_CURRENT
		val url =
			action.editDescription?.takeIf { it.isNotBlank() }
				?: return GuidedAction.ACTION_ID_CURRENT
		Toast.makeText(requireContext(), "Testing connection to $url", Toast.LENGTH_LONG).show()
		lifecycleScope.launch {
			runCatching {
				val client = OwnStreamApiClient(
					url.toString(),
					"OwnStream-Android/${BuildConfig.VERSION_NAME} (Tv-Leanback)"
				)
				return@runCatching withContext(Dispatchers.IO) {
					client.getInfo().response!!
				}
			}.onFailure {
				Log.e("SetupFragmentInstanceUri", "Failed to connect to instance", it)
				Toast.makeText(
					requireContext(),
					"Failed to connect to your instance",
					Toast.LENGTH_LONG
				).show()
			}.onSuccess {
				if (!it.setupComplete) {
					Toast.makeText(
						requireContext(),
						"Instance has not been set up. Please visit your instance in a browser to set it up.",
						Toast.LENGTH_LONG
					).show()
					return@launch
				}
				Toast.makeText(
					requireContext(),
					"Connection success! Instance running ${it.type} version ${it.version}",
					Toast.LENGTH_LONG
				).show()
				val sp = requireContext().getSharedPreferences("main", MODE_PRIVATE)
				sp.edit {
					putString("host", url.toString())
				}
				add(fm, SetupFragmentAccountLogin())
			}
		}
		return GuidedAction.ACTION_ID_CURRENT
	}
}