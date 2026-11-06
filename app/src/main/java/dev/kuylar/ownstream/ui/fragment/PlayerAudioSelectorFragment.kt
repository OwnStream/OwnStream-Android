package dev.kuylar.ownstream.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.kuylar.ownstream.databinding.FragmentPlayerCaptionSelectorBinding
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.databinding.ItemPlayerSettingBinding
import dev.kuylar.recyclerviewbuilder.ExtensibleRecyclerAdapter
import dev.kuylar.recyclerviewbuilder.RecyclerViewBuilder
import java.util.Locale

class PlayerAudioSelectorFragment : BottomSheetDialogFragment() {
	private lateinit var binding: FragmentPlayerCaptionSelectorBinding
	private lateinit var player: ExoPlayer
	private lateinit var adapter: ExtensibleRecyclerAdapter
	private var ready = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		ready = true
		if (this::player.isInitialized) addItems()
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentPlayerCaptionSelectorBinding.inflate(inflater, container, false)
		return binding.root
	}

	fun setPlayer(player: ExoPlayer) {
		this.player = player
		if (ready) addItems()
	}

	@OptIn(UnstableApi::class)
	private fun addItems() {
		adapter = RecyclerViewBuilder(requireContext())
			.addView<AudioOption, ItemPlayerSettingBinding> { binding, item, _ ->
				binding.playerSettingLabel.text = item.label
				binding.playerSettingCheck.visibleIf(item.isSelected, View.INVISIBLE)
				binding.root.setOnClickListener {
					val groups = player.currentTracks.groups
					val group = groups.getOrNull(item.groupIndex) ?: return@setOnClickListener
					if (group.type != C.TRACK_TYPE_AUDIO || !group.isTrackSupported(item.trackIndex)) return@setOnClickListener
					if (item.trackIndex !in 0 until group.mediaTrackGroup.length) return@setOnClickListener

					player.trackSelectionParameters = player.trackSelectionParameters
						.buildUpon()
						.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
						.addOverride(
							TrackSelectionOverride(
								group.mediaTrackGroup,
								listOf(item.trackIndex)
							)
						)
						.build()
					dismiss()
				}
			}
			.build(binding.root)
		player.currentTracks.groups.forEachIndexed { groupIndex, group ->
			if (group.type != C.TRACK_TYPE_AUDIO) return@forEachIndexed

			val mediaTrackGroup = group.mediaTrackGroup
			for (trackIndex in 0 until mediaTrackGroup.length) {
				if (!group.isTrackSupported(trackIndex)) continue
				val format = mediaTrackGroup.getFormat(trackIndex)
				val label = when {
					format.language != null && format.label != null -> getString(R.string.subtitle_selector_template, languageLabel(format.language!!), format.label)
					format.language != null && format.label == null -> languageLabel(format.language!!)
					format.label != null -> format.label!!
					else -> getString(R.string.audio_selector_unlabeled, adapter.items.size + 1)
				}

				adapter.addItem(
					AudioOption(
						groupIndex = groupIndex,
						trackIndex = trackIndex,
						label = label,
						isSelected = group.isTrackSelected(trackIndex)
					)
				)
			}
		}
	}

	private fun languageLabel(languageTag: String): String {
		val locale = Locale.forLanguageTag(languageTag)
		val label = locale.getDisplayName(locale)
		return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
	}

	data class AudioOption(
		val groupIndex: Int,
		val trackIndex: Int,
		val label: String,
		val isSelected: Boolean
	)
}
