package dev.kuylar.ownstream.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.FragmentEpisodeBottomSheetBinding
import dev.kuylar.ownstream.ui.activity.PlayerActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
	private lateinit var binding: FragmentEpisodeBottomSheetBinding
	private val contentId by lazy { arguments?.getString("contentId") }
	private val episodeId by lazy { arguments?.getString("episodeId") }

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentEpisodeBottomSheetBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			loadData()
		}
	}

	private suspend fun loadData() {
		val episode = try {
			client.getEpisode(episodeId!!).response
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load episode", e)
			null
		}
		val video = episode?.videos?.firstOrNull()

		if (episode == null) {
			dismiss()
			return
		}

		binding.title.text = episode.translatedTitle ?: episode.originalTitle
		binding.summary.text = episode.translatedSummary ?: episode.originalSummary
		Glide.with(this).load(episode.thumbnail).into(binding.thumbnail)

		binding.buttonWatch.visibleIf(video != null)
		binding.buttonWatch.setOnClickListener {
			val intent = Intent(requireContext(), PlayerActivity::class.java)
			intent.putExtra("episode", episode.id)
			intent.putExtra("video", video?.id)
			startActivity(intent)
		}
	}
}