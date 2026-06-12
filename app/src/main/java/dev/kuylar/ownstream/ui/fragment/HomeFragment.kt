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
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Shelf
import dev.kuylar.ownstream.databinding.FragmentHomeBinding
import dev.kuylar.ownstream.ui.activity.LoginActivity
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
		val authed = withContext(Dispatchers.IO) {
			try {
				client.whoAmI().responseCode != 401
			} catch (e: Exception) {
				Toast.makeText(requireContext(), R.string.auth_error, Toast.LENGTH_LONG).show()
				Log.e(this.javaClass.name, "Failed to load whoami", e)
				null
			}
		}

		if (authed == false) {
			startActivity(Intent(requireContext(), LoginActivity::class.java))
			activity?.finish()
		}

		val shelves = withContext(Dispatchers.IO) {
			listOfNotNull(
				getShelf("nextUp", getString(R.string.shelf_nextup)),
				getShelf("continue", getString(R.string.shelf_continue)),
				getShelf("recent", getString(R.string.shelf_recents)),
				getShelf("recentContent/movie", getString(R.string.shelf_movies)),
				getShelf("recentContent/tv", getString(R.string.shelf_series)),
			)
		}
		binding.srl.isRefreshing = false

		adapter.submitList(shelves)
	}

	private suspend fun getShelf(id: String, title: String): Shelf? {
		try {
			val items = client.getHomeShelf(id).response ?: emptyList();
			if (items.isEmpty())
				return null

			return Shelf(title, id, null, null, items)
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load shelf: $id", e)
			return null
		}
	}
}