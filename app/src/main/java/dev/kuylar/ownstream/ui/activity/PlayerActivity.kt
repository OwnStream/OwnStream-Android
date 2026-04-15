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
import dev.kuylar.ownstream.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Dispatchers
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
			Log.i(this@PlayerActivity.javaClass.name, "Playing ${mediaItem.localConfiguration?.uri}")
			Log.i(this@PlayerActivity.javaClass.name, "Subtitles: ${mediaItem.localConfiguration?.subtitleConfigurations?.size ?: 0}")
			mediaItem.localConfiguration?.subtitleConfigurations?.forEachIndexed { i, it ->
				Log.i(this@PlayerActivity.javaClass.name, "- [$i/${it.id}] (${it.mimeType}) ${it.language}, ${it.label} @ ${it.uri}")
			}
			player.setMediaItem(mediaItem)
			player.prepare()
			player.play()
		}
	}

	override fun onDestroy() {
		if (this::player.isInitialized) {
			player.stop()
			player.release()
		}
		super.onDestroy()
	}
}