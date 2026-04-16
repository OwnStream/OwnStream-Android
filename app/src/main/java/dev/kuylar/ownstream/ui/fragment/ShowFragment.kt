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
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.FragmentShowBinding
import dev.kuylar.ownstream.ui.activity.PlayerActivity
import dev.kuylar.ownstream.ui.adapter.tab.ShowTabsAdapter
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.equals

@AndroidEntryPoint
class ShowFragment : Fragment() {
	private lateinit var binding: FragmentShowBinding
	private lateinit var id: String

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.getString("id")?.let { id = it }
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentShowBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

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
		binding.buttonWatch.isEnabled = false
		binding.srl.isRefreshing = true
		val show = try {
			val details = client.getContentDetails(id)
			details.takeIf { it.response?.type == "Tv" }
				?: throw Exception("Unexpected content type $id. Expected 'Tv', got '${details.response?.type}'.")
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load movie", e)
			null
		}?.response
		val episodeToWatch = try {
			client.getEpisodeToWatch(id)
		} catch (e: Exception) {
			Log.w(this.javaClass.name, "Failed to load the episode to watch", e)
			null
		}?.response
		binding.srl.isRefreshing = false

		if (show == null) {
			findNavController().popBackStack()
			return
		}

		Glide.with(this).load(show.images.backdrop).into(binding.backdrop)
		Glide.with(this).load(show.images.poster).into(binding.poster)

		val originalTitle = show.originalTitle.takeIf { it.isNotEmpty() }
		val translatedTitle = show.translatedTitle?.takeIf { it.isNotEmpty() }

		when {
			originalTitle != null && translatedTitle != null
					&& !originalTitle.equals(translatedTitle, ignoreCase = true) -> {
				binding.title.text = translatedTitle
				binding.originalTitle.text = originalTitle
				binding.originalTitle.visibility = View.VISIBLE
			}

			translatedTitle != null -> {
				binding.title.text = translatedTitle
			}

			originalTitle != null -> {
				binding.title.text = originalTitle
			}
		}

		binding.tagline.text = show.translatedTagline ?: show.originalTagline

		val isContinue = episodeToWatch?.continueWatching != null
		val episode = episodeToWatch?.continueWatching ?: episodeToWatch?.upNext
		val videoId = episode?.videos?.firstOrNull()?.id
		if (episode != null && videoId != null) {
			binding.buttonWatch.text = getString(
				if (isContinue) R.string.action_continue_episode else R.string.action_watch_episode,
				episode.seasonNumber,
				episode.episodeNumber
			)
			binding.buttonWatch.setOnClickListener {
				lifecycleScope.launch {
					val intent = Intent(requireContext(), PlayerActivity::class.java)
					intent.putExtra("episode", episode.id)
					intent.putExtra("video", videoId)
					startActivity(intent)
				}
			}
			binding.buttonWatch.isEnabled = true
		} else if (episode != null) {
			binding.buttonWatch.setText(R.string.error_video_not_found)
		} else {
			binding.buttonWatch.setText(R.string.login_error_null_response)
		}


		val pagerAdapter = ShowTabsAdapter(this, show)
		binding.viewPager.adapter = pagerAdapter
		TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
			tab.text = when (position) {
				0 -> getString(R.string.show_details)
				else -> getString(R.string.show_episodes)
			}
		}.attach()
	}
}