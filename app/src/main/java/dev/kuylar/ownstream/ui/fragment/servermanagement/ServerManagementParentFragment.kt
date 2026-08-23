package dev.kuylar.ownstream.ui.fragment.servermanagement

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.databinding.FragmentServerManagementParentBinding

class ServerManagementParentFragment : Fragment() {
	private lateinit var binding: FragmentServerManagementParentBinding

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentServerManagementParentBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			childFragmentManager.beginTransaction()
				.replace(R.id.list_pane, ServerManagementItemFragment().apply {
					onCategorySelected = { key -> showDetails(key) }
				})
				.commit()
			showDetails("category_jobs", false)
		}
	}

	private fun showDetails(key: String, open: Boolean = true) {
		val f = when (key) {
			"category_jobs" -> ServerManagementJobsFragment()
			else -> null
		}
		if (f == null) return
		childFragmentManager.beginTransaction()
			.replace(R.id.detail_container, f)
			.commit()
		if (open)
			binding.root.openPane()
	}
}