package dev.kuylar.ownstream.tv.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ItemCardEpisodeBinding

class EpisodeListAdapter(context: Context) :
	ListAdapter<Episode, EpisodeListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Episode>() {
		override fun areItemsTheSame(a: Episode, b: Episode) = a.id == b.id
		override fun areContentsTheSame(a: Episode, b: Episode) = a == b
	}) {
	private val layoutInflater = LayoutInflater.from(context)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		ViewHolder(ItemCardEpisodeBinding.inflate(layoutInflater, parent, false))

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = getItem(position)
		val video = item.videos.firstOrNull()
		with(holder.binding.root) {
			setTitle(item.translatedTitle ?: item.originalTitle)
			val summary = item.translatedSummary ?: item.originalSummary
			if (summary.isNotBlank())
				setDescription(summary)
			if (video == null) {
				setExtraText(context.getString(R.string.content_video_unavailable))
			} else {
				setExtraText(
					context.getString(
						R.string.content_episode_extra_template,
						item.runtime,
						video.width,
						video.height
					)
				)
				setOnClickListener {
					Toast.makeText(context, "Play: ${video.id}", Toast.LENGTH_LONG).show()
				}
			}
			Glide.with(this).load(item.thumbnail).into(getImageView()!!)
		}
	}

	class ViewHolder(val binding: ItemCardEpisodeBinding) :
		RecyclerView.ViewHolder(binding.root)
}