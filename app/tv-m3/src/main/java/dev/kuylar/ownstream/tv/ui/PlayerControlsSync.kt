package dev.kuylar.ownstream.tv.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import dev.kuylar.leanbackm3views.player.PlayerAction
import dev.kuylar.leanbackm3views.player.PlayerControlsView
import dev.kuylar.ownstream.tv.R

class PlayerControlsSync(
	private val player: Player,
	private val controls: PlayerControlsView,
	private val lifecycleOwner: LifecycleOwner,
	private val openSwitcher: (Int) -> Unit
) : DefaultLifecycleObserver, Player.Listener {

	private val handler = Handler(Looper.getMainLooper())
	private val ticker = object : Runnable {
		override fun run() {
			if (!player.isPlaying) return
			pushProgress()
			handler.postDelayed(this, PROGRESS_INTERVAL_MS)
		}
	}

	init {
		controls.onPlay = { player.play() }
		controls.onPause = { player.pause() }
		controls.onSeek = { positionMs -> player.seekTo(positionMs) }

		player.addListener(this)
		lifecycleOwner.lifecycle.addObserver(this)
		syncAll()
	}

	override fun onStart(owner: LifecycleOwner) {
		syncAll()
		startTicker()
	}

	override fun onStop(owner: LifecycleOwner) {
		stopTicker()
	}

	override fun onEvents(player: Player, events: Player.Events) {
		syncAll()
	}

	override fun onIsPlayingChanged(isPlaying: Boolean) {
		controls.setPlaying(isPlaying)
		if (isPlaying) startTicker() else {
			stopTicker()
			pushProgress()
		}
	}

	override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
		controls.setTitle(mediaMetadata.title)
		controls.setSubtitle(mediaMetadata.subtitle)
	}

	override fun onTracksChanged(tracks: Tracks) {
		super.onTracksChanged(tracks)
		var audioTrackCount = 0
		var textTrackCount = 0
		for (trackGroup in tracks.groups) {
			val trackType = trackGroup.type
			if (!trackGroup.isSupported) continue
			when (trackType) {
				C.TRACK_TYPE_AUDIO -> {
					audioTrackCount++
				}

				C.TRACK_TYPE_TEXT -> {
					textTrackCount++
				}
			}
		}
		Log.i("PlayerControlsSync", "Audio: $audioTrackCount, Text: $textTrackCount")
		val showAudioButton = audioTrackCount > 1
		val showSubtitleButton = textTrackCount > 0
		updateSecondaryActions(showAudioButton, showSubtitleButton)
	}

	private fun updateSecondaryActions(showAudioButton: Boolean, showSubtitleButton: Boolean) {
		val actions = mutableListOf<PlayerAction>()
		if (showAudioButton)
			actions.add(
				PlayerAction(
					R.drawable.ic_player_audio_track,
					controls.context.getString(R.string.video_player_dub),
					true
				) { openSwitcher(1) })
		if (showSubtitleButton)
			actions.add(
				PlayerAction(
					R.drawable.ic_player_subtitle_track,
					controls.context.getString(R.string.video_player_sub),
					true
				) { openSwitcher(2) })
		controls.setSecondaryActions(actions)
	}

	fun release() {
		stopTicker()
		player.removeListener(this)
		lifecycleOwner.lifecycle.removeObserver(this)
		controls.onPlay = null
		controls.onPause = null
		controls.onSeek = null
	}

	private fun syncAll() {
		controls.setPlaying(player.isPlaying)
		controls.setDuration(player.durationMs())
		controls.setSeekable(
			player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
		)
		pushProgress()
	}

	private fun pushProgress() {
		controls.setPosition(player.currentPosition)
		controls.setBufferedPosition(player.bufferedPosition)
	}

	private fun startTicker() {
		handler.removeCallbacks(ticker)
		handler.post(ticker)
	}

	private fun stopTicker() {
		handler.removeCallbacks(ticker)
	}

	private fun Player.durationMs(): Long =
		duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L

	private companion object {
		const val PROGRESS_INTERVAL_MS = 100L
	}
}