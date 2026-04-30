package dev.kuylar.ownstream.tvleanback.ui.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tvleanback.ui.playback.CustomPlaybackControlGlue
import dev.kuylar.ownstream.tvleanback.ui.playback.Media3ExoPlayerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackVideoFragment : VideoSupportFragment(), SubtitleSelectorDialogFragment.Host {
	@Inject
	lateinit var client: OwnStreamApiClient
	private lateinit var playerAdapter: Media3ExoPlayerAdapter

	private lateinit var mTransportControlGlue: CustomPlaybackControlGlue
	private var subtitleView: SubtitleView? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setBackgroundResource(android.R.color.black);

		val root = view as? ViewGroup ?: return
		val subtitleOverlay = SubtitleView(requireContext()).apply {
			setUserDefaultStyle()
			setUserDefaultTextSize()
			setFractionalTextSize(0.05f)
		}
		val params = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.WRAP_CONTENT,
			Gravity.BOTTOM
		)
		root.addView(subtitleOverlay, params)
		subtitleView = subtitleOverlay
		playerAdapter.setSubtitleCueListener { cues ->
			subtitleView?.setCues(cues)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val videoId = activity?.intent?.getStringExtra("videoId")
		if (videoId == null) {
			activity?.finish()
			return
		}
		loadVideo(videoId)

		val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
		playerAdapter = Media3ExoPlayerAdapter(requireContext())
		playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

		mTransportControlGlue =
			CustomPlaybackControlGlue(requireContext(), intArrayOf(1, 2, 4, 8), playerAdapter) {
				showSubtitleSelector()
			}
		mTransportControlGlue.host = glueHost
		mTransportControlGlue.playWhenPrepared()
	}

	private fun loadVideo(videoId: String) {
		lifecycleScope.launch {
			val video = withContext(Dispatchers.IO) {
				client.getVideo(videoId).response
			}

			if (video == null) {
				activity?.finish()
				return@launch
			}

			mTransportControlGlue.title = video.content?.translatedTitle
				?: video.content?.originalTitle
						?: "Video"
			mTransportControlGlue.subtitle = if (video.content?.type == "Tv")
				getString(
					dev.kuylar.ownstream.tvleanback.R.string.video_episode_template,
					video.episode?.seasonNumber,
					video.episode?.episodeNumber,
					video.episode?.translatedTitle ?: video.episode?.originalTitle
				) else null
			playerAdapter.setDataSource(video, client)
		}
	}

	override fun onPause() {
		super.onPause()
		mTransportControlGlue.pause()
	}

	override fun onDestroyView() {
		playerAdapter.setSubtitleCueListener(null)
		subtitleView = null
		super.onDestroyView()
	}

	override fun getSubtitleOptions(): List<Media3ExoPlayerAdapter.SubtitleOption> {
		return playerAdapter.getSubtitleOptions()
	}

	override fun areSubtitlesDisabled(): Boolean {
		return playerAdapter.areSubtitlesDisabled()
	}

	override fun onSubtitleDisabled() {
		playerAdapter.disableSubtitles()
	}

	override fun onSubtitleSelected(option: Media3ExoPlayerAdapter.SubtitleOption) {
		playerAdapter.selectSubtitle(option)
	}

	private fun showSubtitleSelector() {
		if (childFragmentManager.findFragmentByTag(SubtitleSelectorDialogFragment.TAG) != null) return
		SubtitleSelectorDialogFragment().show(childFragmentManager, SubtitleSelectorDialogFragment.TAG)
	}
}
