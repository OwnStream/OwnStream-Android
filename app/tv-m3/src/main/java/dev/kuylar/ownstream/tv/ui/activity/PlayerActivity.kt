package dev.kuylar.ownstream.tv.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Video
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivityPlayerBinding
import dev.kuylar.ownstream.tv.ui.PlayerControlsSync
import dev.kuylar.ownstream.tv.ui.adapter.TrackItem
import dev.kuylar.ownstream.tv.ui.adapter.TrackListAdapter
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
	private lateinit var binding: ActivityPlayerBinding
	private lateinit var player: ExoPlayer
	private lateinit var controlsSync: PlayerControlsSync
	private lateinit var backPressedCallback: OnBackPressedCallback
	private lateinit var trackAdapter: TrackListAdapter
	private var progressUpdateJob: Job? = null

	@Inject
	lateinit var client: OwnStreamApiClient

	@OptIn(UnstableApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val id = intent.getStringExtra("videoId")
		if (id == null) {
			finish()
			return
		}

		binding = ActivityPlayerBinding.inflate(layoutInflater)
		setContentView(binding.root)

		trackAdapter = TrackListAdapter(this::onTrackSelected)
		binding.trackList.layoutManager = LinearLayoutManager(this)
		binding.trackList.adapter = trackAdapter

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
			subtitleView = binding.player.subtitleView
		)
		controlsSync = PlayerControlsSync(player, binding.controls, this, this::openSwitcher)
		binding.player.player = player

		backPressedCallback = object : OnBackPressedCallback(false) {
			override fun handleOnBackPressed() {
				if (binding.root.isDrawerOpen(GravityCompat.END))
					binding.root.closeDrawer(GravityCompat.END)
				else {
					backPressedCallback.isEnabled = false
					onBackPressedDispatcher.onBackPressed()
				}
			}
		}
		onBackPressedDispatcher.addCallback(backPressedCallback)

		binding.root.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
			override fun onDrawerOpened(drawerView: View) {
				binding.trackList.post {
					val position = trackAdapter.currentList
						.indexOfFirst { it.isSelected }
						.takeIf { it >= 0 } ?: 0
					binding.trackList.scrollToPosition(position)
					binding.trackList.post {
						binding.trackList.requestFocus()
						binding.trackList.layoutManager
							?.findViewByPosition(position)
							?.requestFocus()
					}
				}
			}

			override fun onDrawerClosed(drawerView: View) {
				backPressedCallback.isEnabled = false
				binding.controls.requestFocus()
				binding.controls.showControls()
			}
		})

		lifecycleScope.launch { loadMedia(id) }
	}

	@OptIn(UnstableApi::class)
	private suspend fun loadMedia(videoId: String) {
		val video = client.getVideo(videoId)
		val progressResp = client.getProgress(videoId).response
		if (video.responseCode != 200) {
			// TODO: Error page
			finish()
			return
		}

		if (video.response == null) {
			finish()
			return
		}

		val mediaItem = MediaItem.Builder().apply {
			setMediaId(videoId)
			setCustomCacheKey(videoId)
			setUri(client.getMediaUrl(videoId, null, "master.m3u8"))
			video.response?.subtitles?.let { subs ->
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
			setMediaMetadata(MediaMetadata.Builder().apply {
				when (video.response?.content?.type) {
					"Movie" -> {
						setTitle(
							video.response?.content?.title ?: video.response?.content?.originalTitle
						)
					}

					"Tv" -> {
						val episode = video.response!!.episode!!
						setTitle(
							video.response?.content?.title ?: video.response?.content?.originalTitle
						)
						setSubtitle(
							getString(
								R.string.video_player_episode_template,
								episode.seasonNumber,
								episode.episodeNumber,
								episode.translatedTitle ?: episode.originalTitle
							)
						)
					}

					else -> getString(R.string.video_player_default_title)
				}
			}.build())
		}.build()
		player.setMediaItem(mediaItem)
		player.playWhenReady = true
		player.prepare()
		player.play()
		progressResp?.position?.toLong()?.let { player.seekTo(it) }
		startProgressSync(video.response!!)
	}

	private fun openSwitcher(type: Int) {
		when (type) {
			1 -> populateAudioTracks()
			2 -> populateTextTracks()
			else -> return
		}
		binding.root.openDrawer(GravityCompat.END)
		binding.controls.hideControls(true)
		backPressedCallback.isEnabled = true
	}

	@OptIn(UnstableApi::class)
	private fun populateAudioTracks() {
		val items = mutableListOf<TrackItem>()
		player.currentTracks.groups.forEachIndexed { groupIndex, group ->
			if (group.type != C.TRACK_TYPE_AUDIO) return@forEachIndexed

			val mediaTrackGroup = group.mediaTrackGroup
			for (trackIndex in 0 until mediaTrackGroup.length) {
				if (!group.isTrackSupported(trackIndex)) continue
				val format = mediaTrackGroup.getFormat(trackIndex)
				val label = when {
					format.language != null && format.label != null -> getString(
						R.string.audio_selector_template,
						languageLabel(format.language!!),
						format.label
					)

					format.language != null -> languageLabel(format.language!!)
					format.label != null -> format.label!!
					else -> getString(R.string.audio_selector_unlabeled, items.size + 1)
				}
				items.add(
					TrackItem(
						groupIndex = groupIndex,
						trackIndex = trackIndex,
						label = label,
						isSelected = group.isTrackSelected(trackIndex)
					)
				)
			}
		}
		submitTracks(items)
	}

	@OptIn(UnstableApi::class)
	private fun populateTextTracks() {
		val items = mutableListOf<TrackItem>()
		var selected = false
		player.currentTracks.groups.forEachIndexed { groupIndex, group ->
			if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed

			val mediaTrackGroup = group.mediaTrackGroup
			for (trackIndex in 0 until mediaTrackGroup.length) {
				if (!group.isTrackSupported(trackIndex)) continue
				if (group.isTrackSelected(trackIndex)) selected = true
				val format = mediaTrackGroup.getFormat(trackIndex)
				val label = when {
					format.language != null && format.label != null -> getString(
						R.string.subtitle_selector_template,
						languageLabel(format.language!!),
						format.label
					)

					format.language != null -> languageLabel(format.language!!)
					format.label != null -> format.label!!
					else -> getString(R.string.subtitle_selector_unlabeled, items.size + 1)
				}
				items.add(
					TrackItem(
						groupIndex = groupIndex,
						trackIndex = trackIndex,
						label = label,
						isSelected = group.isTrackSelected(trackIndex)
					)
				)
			}
		}
		items.add(
			0,
			TrackItem(
				groupIndex = -1,
				trackIndex = -1,
				label = getString(R.string.none),
				isSelected = !selected
			)
		)
		submitTracks(items)
	}

	private fun submitTracks(items: List<TrackItem>) {
		trackAdapter.submitList(items)
	}

	@OptIn(UnstableApi::class)
	private fun onTrackSelected(item: TrackItem) {
		val params = player.trackSelectionParameters.buildUpon()
		if (item.groupIndex < 0) {
			params
				.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
				.clearOverridesOfType(C.TRACK_TYPE_TEXT)
		} else {
			val group = player.currentTracks.groups.getOrNull(item.groupIndex)
				?: return
			if (item.trackIndex !in 0 until group.mediaTrackGroup.length) return
			if (!group.isTrackSupported(item.trackIndex)) return
			if (group.type == C.TRACK_TYPE_TEXT)
				params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
			params
				.clearOverridesOfType(group.type)
				.addOverride(
					TrackSelectionOverride(
						group.mediaTrackGroup,
						listOf(item.trackIndex)
					)
				)
		}
		player.trackSelectionParameters = params.build()
		submitTracks(
			trackAdapter.currentList.map {
				it.copy(
					isSelected = it.groupIndex == item.groupIndex &&
							it.trackIndex == item.trackIndex
				)
			}
		)
	}

	private fun languageLabel(languageTag: String): String {
		val locale = Locale.forLanguageTag(languageTag)
		val label = locale.getDisplayName(locale)
		return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
	}

	private fun startProgressSync(video: Video) {
		progressUpdateJob?.cancel()
		progressUpdateJob = lifecycleScope.launch {
			while (isActive) {
				delay(5.seconds)
				sendProgressUpdate(video)
			}
		}
	}

	private fun sendProgressUpdate(video: Video) {
		if (!this::player.isInitialized || !player.isPlaying) return

		val duration = player.duration
		val position = player.currentPosition

		if (duration <= 0 || position < 0) return

		lifecycleScope.launch(Dispatchers.IO) {
			try {
				val finished = video.segments?.lastOrNull { it.type == "Ending" }
					?.let { position > it.startMilliseconds }
					?: ((position.toFloat() / duration.toFloat()) > .9)
				client.updateWatchProgress(video.id, duration.toInt(), position.toInt(), finished)
			} catch (e: Exception) {
				Log.w(this.javaClass.name, "Failed to update watch progress", e)
			}
		}
	}

	override fun onDestroy() {
		progressUpdateJob?.cancel()
		controlsSync.release()
		player.release()
		super.onDestroy()
	}

	fun <T> Map<String, T>.firstOf(vararg keys: String): T? {
		keys.forEach {
			if (containsKey(it)) return get(it)
		}
		return null
	}
}