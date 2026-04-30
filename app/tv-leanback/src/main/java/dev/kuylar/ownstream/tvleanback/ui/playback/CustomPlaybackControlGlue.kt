package dev.kuylar.ownstream.tvleanback.ui.playback

import android.content.Context
import androidx.leanback.media.PlaybackBannerControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow

class CustomPlaybackControlGlue(
	context: Context,
	seekSpeeds: IntArray,
	private val player: Media3ExoPlayerAdapter
) : PlaybackBannerControlGlue<Media3ExoPlayerAdapter>(context, seekSpeeds, player) {
	private val fastForwardAction = PlaybackControlsRow.FastForwardAction(context, 1)
	private val rewindAction = PlaybackControlsRow.RewindAction(context, 1)
	private val captionsAction = PlaybackControlsRow.ClosedCaptioningAction(context, 1)
	private val resolutionAction = PlaybackControlsRow.HighQualityAction(context, 1)

	override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
		primaryActionsAdapter.apply {
			add(captionsAction)
			add(rewindAction)
		}
		super.onCreatePrimaryActions(primaryActionsAdapter)
		primaryActionsAdapter.apply {
			add(fastForwardAction)
			add(resolutionAction)
		}
	}

	override fun onActionClicked(action: Action) {
		when (action) {
			fastForwardAction -> {
				player.seekTo(player.currentPosition + 5000)
			}

			rewindAction -> {
				player.seekTo(player.currentPosition - 5000)
			}

			captionsAction -> {

			}

			resolutionAction -> {

			}

			else -> super.onActionClicked(action)
		}
	}
}