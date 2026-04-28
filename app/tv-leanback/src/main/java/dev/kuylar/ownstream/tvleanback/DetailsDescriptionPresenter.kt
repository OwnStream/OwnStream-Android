package dev.kuylar.ownstream.tvleanback

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import dev.kuylar.ownstream.api.models.Content

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

	override fun onBindDescription(
		viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder,
		item: Any
	) {
		val movie = item as Content

		// TODO: Localization
		viewHolder.title.text = movie.translatedTitle
		viewHolder.subtitle.text = movie.translatedTagline
		viewHolder.body.text = movie.translatedDescription
	}
}