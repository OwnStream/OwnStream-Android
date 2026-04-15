package dev.kuylar.ownstream.ui.adapter.list

import android.view.ViewGroup
import androidx.core.text.trimmedLength
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.api.models.Shelf
import dev.kuylar.ownstream.databinding.ItemShelfBinding
import dev.kuylar.recyclerviewbuilder.MarginItemDecoration

class ShelvesListAdapter(val fragment: Fragment) :
	ListAdapter<Shelf, ShelvesListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Shelf>() {
		override fun areItemsTheSame(oldItem: Shelf, newItem: Shelf) =
			oldItem.type == newItem.type && oldItem.title == newItem.title

		override fun areContentsTheSame(oldItem: Shelf, newItem: Shelf) = oldItem == newItem
	}) {

	override fun getItemViewType(position: Int) = when (getItem(position).type) {
		else -> 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
		else -> ViewHolder(ItemShelfBinding.inflate(fragment.layoutInflater, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) =
		holder.bind(getItem(position), fragment)

	class ViewHolder(private val binding: ItemShelfBinding) :
		RecyclerView.ViewHolder(binding.root) {
		fun bind(item: Shelf, fragment: Fragment) {
			binding.icon.visibleIf(item.icon != null)
			binding.subtitle.visibleIf((item.description?.trimmedLength() ?: 0) > 0)
			binding.more.visibleIf(false)

			Glide.with(binding.root).load(item.icon).into(binding.icon)
			binding.title.text = item.title
			binding.subtitle.text = item.description

			binding.recycler.layoutManager =
				LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
			//binding.recycler.addItemDecoration(
			//	MarginItemDecoration(
			//		0,
			//		binding.root.context.resources.displayMetrics.densityDpi * 8
			//	)
			//)
			val adapter = ShelfListAdapter(fragment)
			binding.recycler.adapter = adapter
			adapter.submitList(item.items)
		}
	}
}