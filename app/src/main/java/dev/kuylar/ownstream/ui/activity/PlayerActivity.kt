package dev.kuylar.ownstream.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.vkay94.dtpv.DoubleTapPlayerView
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.github.vkay94.timebar.YouTubeSegment
import com.github.vkay94.timebar.YouTubeTimeBar
import com.github.vkay94.timebar.YouTubeTimeBarPreview
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.firstOf
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.Utils.visibleIfNotBlank
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.PreviewFile
import dev.kuylar.ownstream.api.models.Video
import dev.kuylar.ownstream.databinding.ActivityPlayerBinding
import dev.kuylar.ownstream.ui.SkippableSegment
import dev.kuylar.ownstream.ui.StoryboardTransformation
import dev.kuylar.ownstream.ui.fragment.PlayerCaptionSelectorFragment
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity(), Player.Listener, YouTubeTimeBarPreview.Listener {
	private lateinit var binding: ActivityPlayerBinding
	private lateinit var player: ExoPlayer
	private lateinit var videoId: String
	private lateinit var episodeId: String
	private lateinit var video: Video
	private lateinit var episode: Episode
	private var progressUpdateJob: Job? = null
	private var segmentCheckJob: Job? = null

	private lateinit var playPauseButton: MaterialButton
	private lateinit var timebar: YouTubeTimeBar
	private lateinit var timebarPreview: YouTubeTimeBarPreview
	private lateinit var skipButton: MaterialButton
	private lateinit var title: TextView
	private lateinit var subtitle: TextView

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
		player = ExoPlayer.Builder(this).apply {
			setHandleAudioBecomingNoisy(true)
			setAudioAttributes(
				AudioAttributes.Builder().apply {
					setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
					setUsage(C.USAGE_MEDIA)
					setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
				}.build(),
				true
			)
		}.buildWithAssSupport(
			this,
			AssRenderType.OVERLAY_OPEN_GL,
			subtitleView = binding.playerView.subtitleView
		)
		binding.playerView.player = player
		binding.playerView.setControllerAnimationEnabled(false)
		timebar = findViewById(androidx.media3.ui.R.id.exo_progress)
		timebarPreview = findViewById(R.id.player_storyboard)
		timebar.timeBarPreview(timebarPreview)
		timebarPreview.previewListener(this)
		playPauseButton = findViewById(R.id.player_play_pause)
		skipButton = findViewById(R.id.player_skip)
		title = findViewById(R.id.player_title)
		subtitle = findViewById(R.id.player_subtitle)
		setButtons()
		player.addListener(this)
		binding.playerOverlay
			.seekSeconds(5)
			.player(player)
			.performListener(object : YouTubeOverlay.PerformListener {
				override fun onAnimationStart() {
					binding.playerView.useController = false
					binding.playerOverlay.visibility = View.VISIBLE
				}

				override fun onAnimationEnd() {
					binding.playerOverlay.visibility = View.GONE
					binding.playerView.useController = true
				}
			})
		binding.playerView.controller(binding.playerOverlay)

		lifecycleScope.launch {
			val videoResp = withContext(Dispatchers.IO) {
				client.getVideo(videoId).response
			}
			val episodeResp = videoResp?.episode ?: withContext(Dispatchers.IO) {
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
			title.text = video.content?.let { it.translatedTitle ?: it.originalTitle }
				?: getString(R.string.video)
			if (video.content?.type == "Tv")
				video.episode?.let {
					subtitle.text = getString(
						R.string.player_episode_template,
						it.seasonNumber,
						it.episodeNumber,
						it.translatedTitle ?: it.originalTitle
					)
				}
			subtitle.visibleIfNotBlank()
			val mediaItem = MediaItem.Builder().apply {
				setMediaId(videoId)
				setCustomCacheKey(videoId)
				setUri(client.getMediaUrl(videoId, null, "master.m3u8"))
				video.subtitles?.let { subs ->
					setSubtitleConfigurations(
						subs
							.mapNotNull {
								val selected = it.files.firstOf("ass", "ssa", "srt", "vtt")
									?: return@mapNotNull null
								it.copy(
									files = mapOf("sub" to selected)
								)
							}
							.map {
								MediaItem.SubtitleConfiguration.Builder(
									client.getMediaUrl(
										videoId,
										"captions",
										it.files.values.first(),
										mapOf("includeFonts" to "true")
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
											"ass" -> MimeTypes.TEXT_SSA
											"ssa" -> MimeTypes.TEXT_SSA
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
			video.segments?.let { segs ->
				timebar.segments = segs.map { SkippableSegment.fromSegment(it) }
			}
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			startProgressSync()
			startSegmentCheck()

			// Cache preview images for faster loading
			video.previewFiles?.forEach { previewFile ->
				for (i in 0 until previewFile.frameCount) {
					Glide.with(this@PlayerActivity)
						.load(
							client.getMediaUrl(
								videoId,
								"trickplay",
								previewFile.template.replace("%d", (i + 1).toString())
							)
						)
						.preload()
				}
			}
		}
	}

	private fun startProgressSync() {
		progressUpdateJob?.cancel()
		progressUpdateJob = lifecycleScope.launch {
			while (isActive) {
				delay(5.seconds)
				sendProgressUpdate()
			}
		}
	}

	private fun startSegmentCheck() {
		segmentCheckJob?.cancel()
		segmentCheckJob = lifecycleScope.launch {
			while (isActive) {
				delay(100.milliseconds)
				withContext(Dispatchers.Main) {
					onSegmentChanged(timebar.segments.firstOrNull { player.currentPosition in it.startTimeMs..it.endTimeMs } as? SkippableSegment)
				}
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

	private fun setButtons() {
		findViewById<MaterialButton>(R.id.player_play_pause).setOnClickListener {
			if (player.isPlaying) player.pause() else player.play()
		}
		findViewById<MaterialButton>(R.id.player_rew).setOnClickListener {
			player.seekTo(player.currentPosition - 5000)
		}
		findViewById<MaterialButton>(R.id.player_ffwd).setOnClickListener {
			player.seekTo(player.currentPosition + 5000)
		}
		findViewById<MaterialButton>(R.id.player_settings).setOnClickListener {
			Toast.makeText(this, "todo", Toast.LENGTH_SHORT).show()
			//PlayerSettingsFragment(player).show(fragmentManager, null)
		}
		findViewById<MaterialButton>(R.id.player_captions).setOnClickListener {
			if (player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT })
				PlayerCaptionSelectorFragment().apply {
					setPlayer(player)
					show(supportFragmentManager, null)
				}
		}
		findViewById<MaterialButton>(R.id.player_audio).setOnClickListener {
			Toast.makeText(this, "todo", Toast.LENGTH_SHORT).show()
			//PlayerSettingsFragment(player, "audio").show(fragmentManager, null)
		}
		findViewById<MaterialButton>(R.id.player_back).setOnClickListener {
			player.stop()
			player.release()
			finish()
		}
	}

	override fun onPause() {
		super.onPause()
		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		sendProgressUpdate()
	}

	@OptIn(UnstableApi::class)
	override fun onDestroy() {
		progressUpdateJob?.cancel()
		segmentCheckJob?.cancel()
		if (this::player.isInitialized && !player.isReleased) {
			player.stop()
			player.release()
		}
		super.onDestroy()
	}

	override fun onEvents(
		player: Player,
		events: Player.Events
	) {
		super.onEvents(player, events)
		playPauseButton.icon = AppCompatResources.getDrawable(
			this,
			if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
		)
		if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
			findViewById<MaterialButton>(R.id.player_captions)
				.visibleIf(player.currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT })
		}
		if (player.playbackState == Player.STATE_BUFFERING) {
			playPauseButton.visibility = View.INVISIBLE
			binding.playerBufferingProgress.visibility = View.VISIBLE
		} else {
			playPauseButton.visibility = View.VISIBLE
			binding.playerBufferingProgress.visibility = View.GONE
		}
	}

	private fun onSegmentChanged(newSegment: YouTubeSegment?) {
		val segment = newSegment as? SkippableSegment
		skipButton.visibleIf(segment != null)
		skipButton.text = when (segment?.sourceSegment?.type?.lowercase()) {
			"opening" -> getString(R.string.player_skip_intro)
			"ending" -> getString(R.string.player_skip_outro)
			else -> getString(R.string.player_skip_intermission)
		}
		if (segment != null) {
			skipButton.setOnClickListener {
				player.seekTo(segment.endTimeMs - 500)
			}
		} else {
			skipButton.setOnClickListener(null)
		}
	}

	override fun loadThumbnail(imageView: ImageView, position: Long) {
		if (video.previewFiles.isNullOrEmpty()) return
		try {
			Glide.with(this)
				.load(video.previewFiles!!.last().getImageUrl(position))
				.placeholder(imageView.drawable) // <- awesome solution that removes flickering by using the last rendered image as the placeholder
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.transform(video.previewFiles!!.last().getTransformation(position))
				.into(imageView)
		} catch (e: Exception) {
			Log.e("Storyboard", "Failed to update storyboard", e)
		}
	}

	private fun PreviewFile.getImageUrl(position: Long): String {
		if (period == null) {
			return client.getMediaUrl(videoId, "trickplay", template)
		}
		val period = period ?: (player.duration / 100f);
		val frame = floor(position / 1000 / period);
		val image = floor(frame / (columns * rows)).toInt();
		val fileName = template.replace("%d", (image + 1).toString());
		return client.getMediaUrl(videoId, "trickplay", fileName)
	}

	private fun PreviewFile.getTransformation(position: Long): StoryboardTransformation {
		val period = period ?: (player.duration / 100f);
		val frame = floor(position / 1000 / period);
		val frameInImage = frame % (columns * rows);
		val y = floor(frameInImage / rows).toInt();
		val x = floor(frameInImage % columns).toInt();
		return StoryboardTransformation(x, y, rows, columns)
	}
}