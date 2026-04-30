package dev.kuylar.ownstream.tvleanback.ui.presenter

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import dev.kuylar.ownstream.api.models.Content

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

	override fun onBindDescription(
		viewHolder: ViewHolder,
		item: Any
	) {
		if (item is Content) {
			viewHolder.title.text = item.translatedTitle ?: item.originalTitle
			viewHolder.subtitle.text = item.translatedTagline ?: item.originalTagline
			viewHolder.body.text = item.translatedDescription ?: item.originalDescription
		}
	}
}