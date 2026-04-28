package dev.kuylar.ownstream.tvleanback

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackBannerControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackVideoFragment : VideoSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient
	private lateinit var playerAdapter: Media3ExoPlayerAdapter

	private lateinit var mTransportControlGlue: CustomPlaybackControlGlue

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setBackgroundResource(android.R.color.black);
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
			CustomPlaybackControlGlue(requireContext(), intArrayOf(1, 2, 4, 8), playerAdapter)
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
					R.string.video_episode_template,
					video.episode?.seasonNumber,
					video.episode?.episodeNumber,
					video.episode?.translatedTitle ?: video.episode?.originalTitle
				) else null
			playerAdapter.setDataSource(client.getMediaUrl(videoId, "master.m3u8").toUri())
		}
	}

	override fun onPause() {
		super.onPause()
		mTransportControlGlue.pause()
	}
}