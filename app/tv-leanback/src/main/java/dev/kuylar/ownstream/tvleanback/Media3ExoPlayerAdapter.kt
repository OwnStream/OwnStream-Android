package dev.kuylar.ownstream.tvleanback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.media.SurfaceHolderGlueHost
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import android.view.SurfaceHolder
import kotlin.math.max

class Media3ExoPlayerAdapter(
	context: Context,
	private val updatePeriodMs: Long = 50L
) : PlayerAdapter() {
	private val player = ExoPlayer.Builder(context).build()
	private val mainHandler = Handler(Looper.getMainLooper())
	private var isUpdatingProgress = false

	private val progressUpdater = object : Runnable {
		override fun run() {
			callback?.onCurrentPositionChanged(this@Media3ExoPlayerAdapter)
			callback?.onBufferedPositionChanged(this@Media3ExoPlayerAdapter)
			if (isUpdatingProgress) {
				mainHandler.postDelayed(this, updatePeriodMs)
			}
		}
	}

	private val playerListener = object : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			callback?.onPreparedStateChanged(this@Media3ExoPlayerAdapter)
			callback?.onPlayStateChanged(this@Media3ExoPlayerAdapter)
			callback?.onDurationChanged(this@Media3ExoPlayerAdapter)
			callback?.onBufferedPositionChanged(this@Media3ExoPlayerAdapter)
			callback?.onCurrentPositionChanged(this@Media3ExoPlayerAdapter)
			if (playbackState == Player.STATE_ENDED) {
				callback?.onPlayCompleted(this@Media3ExoPlayerAdapter)
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			callback?.onPlayStateChanged(this@Media3ExoPlayerAdapter)
		}

		override fun onVideoSizeChanged(videoSize: VideoSize) {
			callback?.onVideoSizeChanged(this@Media3ExoPlayerAdapter, videoSize.width,
				max(videoSize.height, 1)
			)
		}

		override fun onPlayerError(error: PlaybackException) {
			callback?.onError(
				this@Media3ExoPlayerAdapter,
				error.errorCode,
				error.localizedMessage ?: error.message ?: "Playback failed"
			)
		}
	}

	private val surfaceHolderCallback = object : SurfaceHolder.Callback {
		override fun surfaceCreated(holder: SurfaceHolder) {
			player.setVideoSurfaceHolder(holder)
		}

		override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

		override fun surfaceDestroyed(holder: SurfaceHolder) {
			player.clearVideoSurfaceHolder(holder)
		}
	}

	init {
		player.addListener(playerListener)
	}

	fun setDataSource(uri: Uri) {
		player.setMediaItem(MediaItem.fromUri(uri))
		player.prepare()
	}

	override fun isPrepared(): Boolean {
		return player.playbackState != Player.STATE_IDLE
	}

	override fun play() {
		player.playWhenReady = true
		player.play()
	}

	override fun pause() {
		player.pause()
	}

	override fun fastForward() {
		player.seekForward()
	}

	override fun rewind() {
		player.seekBack()
	}

	override fun seekTo(positionInMs: Long) {
		player.seekTo(positionInMs)
	}

	override fun isPlaying(): Boolean {
		return player.isPlaying
	}

	override fun getDuration(): Long {
		return player.duration.takeIf { it != C.TIME_UNSET } ?: -1L
	}

	override fun getCurrentPosition(): Long {
		return player.currentPosition
	}

	override fun getBufferedPosition(): Long {
		return player.bufferedPosition
	}

	override fun setProgressUpdatingEnabled(enabled: Boolean) {
		isUpdatingProgress = enabled
		mainHandler.removeCallbacks(progressUpdater)
		if (enabled) {
			mainHandler.post(progressUpdater)
		}
	}

	override fun onAttachedToHost(host: PlaybackGlueHost) {
		if (host is SurfaceHolderGlueHost) {
			host.setSurfaceHolderCallback(surfaceHolderCallback)
		}
		super.onAttachedToHost(host)
	}

	override fun onDetachedFromHost() {
		setProgressUpdatingEnabled(false)
		player.clearVideoSurface()
		player.removeListener(playerListener)
		player.release()
		super.onDetachedFromHost()
	}
}
