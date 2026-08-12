package dev.kuylar.ownstream.ui.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlin.math.max
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.databinding.FragmentLibraryBinding
import dev.kuylar.ownstream.databinding.ItemCardBinding
import dev.kuylar.recyclerviewbuilder.ExtensibleRecyclerAdapter
import dev.kuylar.recyclerviewbuilder.RecyclerViewBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LibraryFragment : Fragment() {
	private lateinit var binding: FragmentLibraryBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	private val libraryChips = emptyMap<String, Chip>().toMutableMap()
	private var libraryId: String = "00000000-0000-0000-0000-000000000000"
	private var typeFilter: String? = null
	private var page = 0
	private var hasMore = true
	private lateinit var layoutManager: GridLayoutManager
	private lateinit var adapter: ExtensibleRecyclerAdapter

	private var loading
		get() = adapter.loading || binding.srl.isRefreshing
		set(value) {
			binding.srl.isRefreshing = value
		}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentLibraryBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.filterTypeMovies.setOnCheckedChangeListener { _, value ->
			if (value) {
				binding.filterTypeTv.isChecked = false
				typeFilter = "movies"
			} else typeFilter = null
			refresh()
		}
		binding.filterTypeTv.setOnCheckedChangeListener { _, value ->
			if (value) {
				binding.filterTypeMovies.isChecked = false
				typeFilter = "tv"
			} else typeFilter = null
			refresh()
		}

		layoutManager = GridLayoutManager(requireContext(), 1)
		adapter = RecyclerViewBuilder(requireContext())
			.addView<Content, ItemCardBinding> { binding, item, _ ->
				binding.title.text = item.translatedTitle ?: item.originalTitle
				binding.subtitle.text = item.releasedAt.take(4)
				Glide.with(this)
					.load(item.images.poster)
					.into(binding.poster)

				// API doesn't have this here yet
				binding.progress.visibility = View.GONE

				binding.root.setOnClickListener {
					when (item.type) {
						"Movie" -> {
							findNavController().navigate(
								R.id.nav_movie,
								Bundle().apply { putString("id", item.id) }
							)
						}

						"Tv" -> {
							findNavController().navigate(
								R.id.nav_show,
								Bundle().apply { putString("id", item.id) }
							)
						}

						else -> {
							Toast.makeText(
								binding.root.context,
								binding.root.context.getString(
									R.string.error_unexpected_type,
									item.type
								),
								Toast.LENGTH_LONG
							).show()
						}
					}
				}
			}
			.setScrollToBottomListener(::loadMore)
			.setLayoutManager(layoutManager)
			.build(binding.recycler)
		adapter.clearItems()

		binding.recycler.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
			updateSpanCount(view.width)
		}

		binding.recycler.post {
			updateSpanCount(binding.recycler.width)
		}

		binding.srl.setOnRefreshListener(::refresh)

		lifecycleScope.launch(Dispatchers.IO) {
			refreshChips()
		}
		refresh()
	}

	private suspend fun refreshChips() {
		val libraries = client.getLibraries().response ?: emptyList()
		if (libraries.size <= 1) { // No need to show a list if there's just one library
			withContext(Dispatchers.Main) {
				binding.divider.visibility = View.GONE
			}
			return
		}
		withContext(Dispatchers.Main) {
			libraries.forEach { library ->
				libraryChips[library.id] = Chip(requireContext()).apply {
					text = library.name
					isCheckable = true
					setOnCheckedChangeListener { _, value ->
						if (value) {
							if (libraryChips.containsKey(libraryId))
								libraryChips[libraryId]?.isChecked = false
							libraryId = library.id
						} else {
							libraryId = "00000000-0000-0000-0000-000000000000"
						}
						refresh()
					}
				}
				binding.filters.addView(libraryChips[library.id])
			}
		}
	}

	private fun refresh() {
		if (loading) return
		adapter.clearItems()
		page = 0
		hasMore = true
		loading = true
		lifecycleScope.launch(Dispatchers.IO) {
			loadAndAddItems()
		}
	}

	private fun loadMore(adapter: ExtensibleRecyclerAdapter) {
		if (loading) return
		page++
		if (!hasMore) return
		loading = true
		lifecycleScope.launch(Dispatchers.IO) {
			loadAndAddItems()
		}
	}

	private suspend fun loadAndAddItems() {
		val response = try {
			client.getLibraryItems(libraryId, typeFilter, page, 20).response!!
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load items", e)
			Snackbar.make(binding.root, R.string.generic_error, Snackbar.LENGTH_SHORT).show()
			return
		}
		hasMore = response.hasMore
		withContext(Dispatchers.Main) {
			adapter.addItems(response.items)
			loading = false
		}
	}

	private fun updateSpanCount(recyclerWidth: Int) {
		if (recyclerWidth <= 0) return
		val columnWidthDp = 125
		val density = resources.displayMetrics.density
		val columnWidthPx = (columnWidthDp * density).toInt()
		val spanCount = max(3, recyclerWidth / columnWidthPx)
		if (layoutManager.spanCount != spanCount) {
			layoutManager.spanCount = spanCount
		}
	}
}
