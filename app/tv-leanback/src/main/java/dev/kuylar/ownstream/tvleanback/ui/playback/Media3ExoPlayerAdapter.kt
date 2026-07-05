package dev.kuylar.ownstream.tvleanback.ui.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.media.SurfaceHolderGlueHost
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Video
import dev.kuylar.ownstream.tvleanback.R
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import java.util.Locale
import kotlin.math.max

class Media3ExoPlayerAdapter(
	private val context: Context,
	private val updatePeriodMs: Long = 50L
) : PlayerAdapter() {
	data class SubtitleOption(
		val groupIndex: Int,
		val trackIndex: Int,
		val label: String,
		val isSelected: Boolean
	)

	private lateinit var player: ExoPlayer
	private val mainHandler = Handler(Looper.getMainLooper())
	private var isUpdatingProgress = false
	private var subtitleCueListener: ((List<Cue>) -> Unit)? = null

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
			callback?.onVideoSizeChanged(
				this@Media3ExoPlayerAdapter,
				videoSize.width,
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

		override fun onCues(cueGroup: CueGroup) {
			subtitleCueListener?.invoke(cueGroup.cues)
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

	@OptIn(UnstableApi::class)
	fun initPlayer(subtitleView: SubtitleView?) {
		player = ExoPlayer.Builder(context).buildWithAssSupport(
			context,
			AssRenderType.OVERLAY_OPEN_GL,
			subtitleView = subtitleView
		)
		player.addListener(playerListener)
	}

	@OptIn(UnstableApi::class)
	fun setDataSource(video: Video, client: OwnStreamApiClient) {
		val mediaItem = MediaItem.Builder().apply {
			setMediaId(video.id)
			setCustomCacheKey(video.id)
			setUri(client.getMediaUrl(video.id, null, "master.m3u8"))
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
									video.id,
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
	}

	override fun isPrepared(): Boolean {
		return this::player.isInitialized && player.playbackState != Player.STATE_IDLE
	}

	override fun play() {
		if (!this::player.isInitialized) return
		player.playWhenReady = true
		player.play()
	}

	override fun pause() {
		if (!this::player.isInitialized) return
		player.pause()
	}

	override fun fastForward() {
		if (!this::player.isInitialized) return
		player.seekForward()
	}

	override fun rewind() {
		if (!this::player.isInitialized) return
		player.seekBack()
	}

	override fun seekTo(positionInMs: Long) {
		if (!this::player.isInitialized) return
		player.seekTo(positionInMs)
	}

	override fun isPlaying(): Boolean {
		if (!this::player.isInitialized) return false
		return player.isPlaying
	}

	override fun getDuration(): Long {
		if (!this::player.isInitialized) return -1L
		return player.duration.takeIf { it != C.TIME_UNSET } ?: -1L
	}

	override fun getCurrentPosition(): Long {
		if (!this::player.isInitialized) return -1L
		return player.currentPosition
	}

	override fun getBufferedPosition(): Long {
		if (!this::player.isInitialized) return -1L
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
		if (this::player.isInitialized) {
			player.clearVideoSurface()
			player.removeListener(playerListener)
			player.release()
		}
		super.onDetachedFromHost()
	}

	@OptIn(UnstableApi::class)
	fun getSubtitleOptions(): List<SubtitleOption> {
		val options = mutableListOf<SubtitleOption>()
		player.currentTracks.groups.forEachIndexed { groupIndex, group ->
			if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed

			val mediaTrackGroup = group.mediaTrackGroup
			for (trackIndex in 0 until mediaTrackGroup.length) {
				if (!group.isTrackSupported(trackIndex)) continue
				val format = mediaTrackGroup.getFormat(trackIndex)
				val label = format.label?.takeIf { it.isNotBlank() }
					?: format.language?.let(::languageLabel)
					?: context.getString(R.string.subtitle_selector_unlabeled, options.size + 1)

				options.add(SubtitleOption(
					groupIndex = groupIndex,
					trackIndex = trackIndex,
					label = label,
					isSelected = group.isTrackSelected(trackIndex)
				))
			}
		}
		return options
	}

	fun areSubtitlesDisabled(): Boolean {
		return player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
	}

	fun setSubtitleCueListener(listener: ((List<Cue>) -> Unit)?) {
		subtitleCueListener = listener
		listener?.invoke(player.currentCues.cues)
	}

	fun disableSubtitles() {
		player.trackSelectionParameters = player.trackSelectionParameters
			.buildUpon()
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
			.clearOverridesOfType(C.TRACK_TYPE_TEXT)
			.build()
	}

	@OptIn(UnstableApi::class)
	fun selectSubtitle(option: SubtitleOption) {
		val groups = player.currentTracks.groups
		val group = groups.getOrNull(option.groupIndex) ?: return
		if (group.type != C.TRACK_TYPE_TEXT || !group.isTrackSupported(option.trackIndex)) return
		if (option.trackIndex !in 0 until group.mediaTrackGroup.length) return

		player.trackSelectionParameters = player.trackSelectionParameters
			.buildUpon()
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
			.clearOverridesOfType(C.TRACK_TYPE_TEXT)
			.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(option.trackIndex)))
			.build()
	}

	private fun languageLabel(languageTag: String): String {
		val locale = Locale.forLanguageTag(languageTag)
		val label = locale.getDisplayName(locale)
		return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
	}

	private fun <T> Map<String, T>.firstOf(vararg keys: String): T? {
		keys.forEach {
			if (containsKey(it)) return get(it)
		}
		return null
	}
}
