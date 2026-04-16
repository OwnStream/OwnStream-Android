package dev.kuylar.ownstream.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.databinding.FragmentShowEpisodesBinding
import dev.kuylar.ownstream.databinding.ItemEpisodeBinding
import dev.kuylar.ownstream.ui.activity.PlayerActivity
import dev.kuylar.recyclerviewbuilder.ExtensibleRecyclerAdapter
import dev.kuylar.recyclerviewbuilder.RecyclerViewBuilder
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ShowEpisodesFragment : Fragment() {
	private lateinit var binding: FragmentShowEpisodesBinding
	private lateinit var adapter: ExtensibleRecyclerAdapter
	private val contentId by lazy { arguments?.getString("contentId") }

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentShowEpisodesBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		adapter = RecyclerViewBuilder(requireContext()).apply {
			addView<Episode, ItemEpisodeBinding> { binding, item, context ->
				binding.title.text = getString(
					R.string.template_episode_title,
					item.episodeNumber,
					item.translatedTitle ?: item.originalTitle
				)
				binding.summary.text = item.translatedSummary ?: item.originalSummary
				binding.progress.max = 100
				binding.progress.progress = item.progress?.roundToInt() ?: 0
				binding.progress.visibleIf(item.progress != null)
				Glide.with(context).load(item.thumbnail).into(binding.thumbnail)
				item.videos.firstOrNull()?.let { video ->
					binding.root.setOnClickListener {
						val intent = Intent(requireContext(), PlayerActivity::class.java)
						intent.putExtra("episode", item.id)
						intent.putExtra("video", video.id)
						startActivity(intent)
					}
				}
			}
			setLoadingItem(R.layout.layout_loading_full)
			setMarginDivider(vertical = (resources.displayMetrics.density * 8).toInt())
		}.build(binding.recycler)

		lifecycleScope.launch {
			refresh()
		}
	}

	private suspend fun refresh() {
		if (contentId == null) return
		val seasons = try {
			client.getSeasons(contentId!!).response ?: emptyList()
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load seasons", e)
			Toast.makeText(
				requireContext(),
				getString(R.string.error_season_load_failed),
				Toast.LENGTH_LONG
			).show()
			emptyList()
		}

		val seasonLabels = seasons.map {
			getString(R.string.template_season_title, it.index)
		}

		val dropdown = binding.seasonPicker.editText as MaterialAutoCompleteTextView

		val adapter = ArrayAdapter(
			requireContext(),
			android.R.layout.simple_list_item_1,
			seasonLabels
		)
		dropdown.setAdapter(adapter)

		dropdown.setOnItemClickListener { _, _, position, _ ->
			val selectedSeason = seasons[position]
			lifecycleScope.launch {
				loadSeasonEpisodes(selectedSeason.index)
			}
		}

		if (seasons.isNotEmpty()) {
			dropdown.setText(seasonLabels.first(), false)
			loadSeasonEpisodes(seasons.first().index)
		}
	}

	private suspend fun loadSeasonEpisodes(season: Int) {
		adapter.clearItems()
		adapter.loading = true
		val seasons = try {
			client.getEpisodes(contentId!!, season).response ?: emptyList()
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load episodes for season $season", e)
			Toast.makeText(
				requireContext(),
				getString(R.string.error_season_load_failed),
				Toast.LENGTH_LONG
			).show()
			emptyList()
		}

		adapter.addItems(seasons)
		adapter.loading = false
	}
}