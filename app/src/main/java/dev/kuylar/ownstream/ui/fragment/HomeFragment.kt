package dev.kuylar.ownstream.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.ApiResponse
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.FragmentHomeBinding
import dev.kuylar.ownstream.ui.activity.LoginActivity
import dev.kuylar.ownstream.ui.adapter.list.ShelfListAdapter
import dev.kuylar.ownstream.ui.adapter.list.ShelvesListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
	private lateinit var binding: FragmentHomeBinding
	private lateinit var adapter: ShelvesListAdapter

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentHomeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		adapter = ShelvesListAdapter(this)
		binding.recycler.layoutManager = LinearLayoutManager(binding.recycler.context)
		binding.recycler.adapter = adapter

		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				refresh()
			}
		}
		binding.srl.setOnRefreshListener {
			lifecycleScope.launch {
				refresh()
			}
		}
	}

	private suspend fun refresh() {
		binding.srl.isRefreshing = true
		val shelves = withContext(Dispatchers.IO) {
			try {
				client.getHomeShelves()
			} catch (e: Exception) {
				Log.e(this.javaClass.name, "Failed to load home shelves", e)
				null
			}
		}
		binding.srl.isRefreshing = false

		if (shelves?.responseCode == 401) {
			startActivity(Intent(requireContext(), LoginActivity::class.java))
			activity?.finish()
			return
		}

		if (shelves == null || shelves.response == null) {
			Toast.makeText(requireContext(), R.string.home_error, Toast.LENGTH_LONG)
				.show()
			return
		}

		adapter.submitList(shelves.response)
	}
}