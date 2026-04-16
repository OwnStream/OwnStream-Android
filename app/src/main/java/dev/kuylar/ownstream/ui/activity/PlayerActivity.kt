package dev.kuylar.ownstream.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.Utils.firstOf
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.Video
import dev.kuylar.ownstream.api.models.WatchProgressResponse
import dev.kuylar.ownstream.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
	private lateinit var binding: ActivityPlayerBinding
	private lateinit var player: ExoPlayer
	private lateinit var videoId: String
	private lateinit var episodeId: String
	private lateinit var video: Video
	private lateinit var episode: Episode
	private var progressUpdateJob: Job? = null

	@Inject
	lateinit var client: OwnStreamApiClient

	@OptIn(UnstableApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		videoId = intent.getStringExtra("video") ?: "null"
		episodeId = intent.getStringExtra("episode") ?: "null"

		if (videoId == "null" || episodeId == "null") {
			finish()
			return
		}

		binding = ActivityPlayerBinding.inflate(layoutInflater)
		setContentView(binding.root)

		WindowInsetsControllerCompat(window, binding.root).let { controller ->
			controller.hide(WindowInsetsCompat.Type.systemBars())
			controller.systemBarsBehavior =
				WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}

		player = ExoPlayer.Builder(this)
			.build()
		binding.playerView.player = player

		lifecycleScope.launch {
			val videoResp = withContext(Dispatchers.IO) {
				client.getVideo(videoId).response
			}
			val episodeResp = withContext(Dispatchers.IO) {
				client.getEpisode(episodeId).response
			}
			val progressResp = withContext(Dispatchers.IO) {
				client.getProgress(videoId).response
			}
			if (videoResp == null || episodeResp == null) {
				finish()
				return@launch
			}

			video = videoResp
			episode = episodeResp
			val mediaItem = MediaItem.Builder().apply {
				setMediaId(videoId)
				setCustomCacheKey(videoId)
				setUri(client.getMediaUrl(videoId, "master.m3u8"))
				video.subtitles?.let { subs ->
					setSubtitleConfigurations(
						subs
							.mapNotNull {
								val selected =
									it.files.firstOf("srt", "vtt") ?: return@mapNotNull null
								it.copy(
									files = mapOf("sub" to selected)
								)
							}
							.map {
								MediaItem.SubtitleConfiguration.Builder(
									client.getMediaUrl(
										videoId,
										"captions",
										it.files.values.first()
									).toUri()
								).apply {
									this.setId(it.id.toString())
									this.setLabel(it.title)
									this.setLanguage(it.language)
									var flags = 0
									if (it.default) flags = flags or C.SELECTION_FLAG_DEFAULT
									if (it.forced) flags = flags or C.SELECTION_FLAG_FORCED
									this.setSelectionFlags(flags)
									val mime =
										when (it.files.values.first().substringAfterLast('.')) {
											"vtt" -> MimeTypes.TEXT_VTT
											"srt" -> MimeTypes.APPLICATION_SUBRIP
											else -> MimeTypes.TEXT_UNKNOWN
										}
									this.setMimeType(mime)
								}.build()
							}

					)
				}
			}.build()
			player.setMediaItem(mediaItem)
			player.prepare()
			progressResp?.position?.toLong()?.let { player.seekTo(it) }
			player.play()
			startProgressSync()
		}
	}

	private fun startProgressSync() {
		progressUpdateJob?.cancel()
		progressUpdateJob = lifecycleScope.launch {
			while (isActive) {
				delay(5_000)
				sendProgressUpdate()
			}
		}
	}

	private fun sendProgressUpdate() {
		if (!this::player.isInitialized || !player.isPlaying) return

		val duration = player.duration
		val position = player.currentPosition

		if (duration <= 0 || position < 0) return

		lifecycleScope.launch(Dispatchers.IO) {
			try {
				val finished = (position.toFloat() / duration.toFloat()) > .9
				client.updateWatchProgress(videoId, duration.toInt(), position.toInt(), finished)
			} catch (e: Exception) {
				Log.w(this.javaClass.name, "Failed to update watch progress", e)
			}
		}
	}

	override fun onPause() {
		super.onPause()
		sendProgressUpdate()
	}

	override fun onDestroy() {
		progressUpdateJob?.cancel()
		if (this::player.isInitialized) {
			player.stop()
			player.release()
		}
		super.onDestroy()
	}
}