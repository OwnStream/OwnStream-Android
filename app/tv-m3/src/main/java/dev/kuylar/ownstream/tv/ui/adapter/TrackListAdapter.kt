package dev.kuylar.ownstream.tv.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.kuylar.leanbackm3views.component.TvListItem
import dev.kuylar.ownstream.tv.databinding.ItemPlayerTrackBinding

class TrackListAdapter(
	private val onTrackSelected: (TrackItem) -> Unit
) : ListAdapter<TrackItem, TrackListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<TrackItem>() {
	override fun areItemsTheSame(a: TrackItem, b: TrackItem) =
		a.groupIndex == b.groupIndex && a.trackIndex == b.trackIndex

	override fun areContentsTheSame(a: TrackItem, b: TrackItem) = a == b
}) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(ItemPlayerTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = getItem(position)
		with(holder.binding.root) {
			setTitle(item.label)
			setControlType(TvListItem.ControlType.Radio)
			setChecked(item.isSelected)
			setOnClickListener { onTrackSelected(item) }
		}
	}

	class ViewHolder(val binding: ItemPlayerTrackBinding) :
		RecyclerView.ViewHolder(binding.root)
}

data class TrackItem(
	val groupIndex: Int,
	val trackIndex: Int,
	val label: String,
	val isSelected: Boolean
)
