package dev.kuylar.ownstream.tv.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.kuylar.ownstream.api.models.Season
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ItemCardSeasonBinding

class SeasonListAdapter(context: Context) :
	ListAdapter<Season, SeasonListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Season>() {
		override fun areItemsTheSame(a: Season, b: Season) = a.index == b.index
		override fun areContentsTheSame(a: Season, b: Season) = a == b
	}) {
	private val layoutInflater = LayoutInflater.from(context)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(ItemCardSeasonBinding.inflate(layoutInflater, parent, false))

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = getItem(position)
		with(holder.binding.root) {
			setTitle(item.translatedTitle ?: item.title ?: context.getString(R.string.content_season_template, item.index))
			setExtraText(context.getString(R.string.content_season_episodes_template, item.episodeCount))
			Glide.with(this).load(item.poster).into(getImageView()!!)
		}
	}

	class ViewHolder(val binding: ItemCardSeasonBinding) :
		RecyclerView.ViewHolder(binding.root)
}