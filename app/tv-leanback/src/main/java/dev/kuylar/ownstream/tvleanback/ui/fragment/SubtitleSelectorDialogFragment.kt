package dev.kuylar.ownstream.tvleanback.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import dev.kuylar.ownstream.tvleanback.R
import dev.kuylar.ownstream.tvleanback.ui.playback.Media3ExoPlayerAdapter
import androidx.core.graphics.drawable.toDrawable

class SubtitleSelectorDialogFragment : DialogFragment() {
	interface Host {
		fun getSubtitleOptions(): List<Media3ExoPlayerAdapter.SubtitleOption>
		fun areSubtitlesDisabled(): Boolean
		fun onSubtitleDisabled()
		fun onSubtitleSelected(option: Media3ExoPlayerAdapter.SubtitleOption)
	}

	companion object {
		const val TAG = "SubtitleSelectorDialog"
	}

	private lateinit var host: Host

	override fun onAttach(context: Context) {
		super.onAttach(context)
		host = (parentFragment as? Host)
			?: (activity as? Host)
			?: error("SubtitleSelectorDialogFragment host must implement Host.")
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.Theme_OwnStream_SubtitleSelectorDialog)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.dialog_subtitle_selector, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val listView = view.findViewById<ListView>(R.id.subtitle_list)
		val subtitleOptions = host.getSubtitleOptions()
		val labels = buildList {
			add(getString(R.string.subtitle_track_off))
			addAll(subtitleOptions.map { it.label })
		}
		listView.choiceMode = ListView.CHOICE_MODE_SINGLE
		listView.adapter = ArrayAdapter(
			requireContext(),
			R.layout.item_subtitle_option,
			android.R.id.text1,
			labels
		)

		val selectedIndex = if (host.areSubtitlesDisabled()) {
			0
		} else {
			val selectedTrackIndex = subtitleOptions.indexOfFirst { it.isSelected }
			if (selectedTrackIndex >= 0) selectedTrackIndex + 1 else 0
		}
		listView.setItemChecked(selectedIndex, true)
		listView.requestFocus()
		listView.setOnItemClickListener { _, _, position, _ ->
			if (position == 0) {
				host.onSubtitleDisabled()
			} else {
				host.onSubtitleSelected(subtitleOptions[position - 1])
			}
			dismiss()
		}
	}

	override fun onStart() {
		super.onStart()

		dialog?.window?.apply {
			setLayout(
				resources.getDimensionPixelSize(R.dimen.subtitle_selector_dialog_width),
				ViewGroup.LayoutParams.MATCH_PARENT
			)
			setGravity(Gravity.END)
			setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
		}
	}
}
