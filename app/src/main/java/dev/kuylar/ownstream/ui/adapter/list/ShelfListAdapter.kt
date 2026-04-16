package dev.kuylar.ownstream.ui.adapter.list

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.api.models.ShelfItem
import dev.kuylar.ownstream.databinding.ItemShelfEntryPosterBinding
import dev.kuylar.ownstream.databinding.ItemShelfEntryThumbnailBinding
import dev.kuylar.ownstream.ui.fragment.EpisodeBottomSheetFragment
import kotlin.math.roundToInt

class ShelfListAdapter(val fragment: Fragment) :
	ListAdapter<ShelfItem, ShelfListAdapter.ViewHolder>(object :
		DiffUtil.ItemCallback<ShelfItem>() {
		override fun areItemsTheSame(oldItem: ShelfItem, newItem: ShelfItem) =
			oldItem.type == newItem.type && oldItem.title == newItem.title

		override fun areContentsTheSame(oldItem: ShelfItem, newItem: ShelfItem) = oldItem == newItem
	}) {

	override fun getItemViewType(position: Int) = when (getItem(position).type) {
		"episode", "video" -> 1
		else -> 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
		1 -> ViewHolder(
			ItemShelfEntryPosterBinding.bind(
				ItemShelfEntryThumbnailBinding.inflate(
					fragment.layoutInflater,
					parent,
					false
				).root
			)
		)

		else -> ViewHolder(
			ItemShelfEntryPosterBinding.inflate(
				fragment.layoutInflater,
				parent,
				false
			)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) =
		holder.bind(getItem(position), fragment)

	class ViewHolder(private val binding: ItemShelfEntryPosterBinding) :
		RecyclerView.ViewHolder(binding.root) {
		fun bind(item: ShelfItem, fragment: Fragment) {
			Glide.with(binding.root).load(item.image).into(binding.poster)
			binding.title.text = item.title
			binding.subtitle.text = item.subtitle.joinToString(" \u2022 ");
			binding.progress.max = 100
			binding.progress.progress = item.watchProgress?.roundToInt() ?: 0
			binding.progress.visibleIf(item.watchProgress != null)

			binding.root.setOnClickListener {
				when (item.type) {
					"movie" -> {
						fragment.findNavController().navigate(
							R.id.nav_movie,
							Bundle().apply { putString("id", item.id) }
						)
					}

					"tv" -> {
						fragment.findNavController().navigate(
							R.id.nav_show,
							Bundle().apply { putString("id", item.id) }
						)
					}

					"episode" -> {
						val f = EpisodeBottomSheetFragment()
						f.arguments = Bundle().apply {
							putString("contentId", item.id)
							putString("episodeId", item.episodeId)
						}
						f.show(fragment.parentFragmentManager, "episodeBottomSheet")
					}

					else -> {
						Toast.makeText(
							binding.root.context,
							binding.root.context.getString(
								R.string.error_unexpected_type,
								item.type
							),
							Toast.LENGTH_LONG
						).show()
					}
				}
			}
		}
	}
}