package dev.kuylar.ownstream.tvleanback

import android.graphics.drawable.Drawable
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.ViewGroup

import com.bumptech.glide.Glide
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.Season
import dev.kuylar.ownstream.api.models.ShelfItem
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter : Presenter() {
	private var mDefaultCardImage: Drawable? = null
	private var sSelectedBackgroundColor: Int by Delegates.notNull()
	private var sDefaultBackgroundColor: Int by Delegates.notNull()

	override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
		Log.d(TAG, "onCreateViewHolder")

		sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
		sSelectedBackgroundColor =
			ContextCompat.getColor(parent.context, R.color.selected_background)
		mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.drawable.movie)

		val cardView = object : ImageCardView(parent.context) {
			override fun setSelected(selected: Boolean) {
				updateCardBackgroundColor(this, selected)
				super.setSelected(selected)
			}
		}

		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true
		updateCardBackgroundColor(cardView, false)
		return Presenter.ViewHolder(cardView)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		when (item) {
			is ShelfItem -> {
				val cardView = viewHolder.view as ImageCardView

				Log.d(TAG, "onBindViewHolder")
				if (item.image != null) {
					cardView.titleText = item.title
					cardView.contentText = item.subtitle.joinToString(" \u2022 ")
					cardView.setMainImageDimensions(
						if (item.type == "episode" || item.type == "video") CARD_WIDTH_LANDSCAPE else CARD_WIDTH_PORTRAIT,
						CARD_HEIGHT
					)
					Glide.with(viewHolder.view.context)
						.load(item.image)
						.centerCrop()
						.error(mDefaultCardImage)
						.into(cardView.mainImageView!!)
				}
			}

			is Episode -> {
				val cardView = viewHolder.view as ImageCardView
				cardView.titleText =
					viewHolder.view.context.getString(R.string.episode_title_template, item.episodeNumber, item.translatedTitle)
				cardView.contentText =
					viewHolder.view.context.getString(R.string.episode_content_template, item.runtime)
				cardView.setMainImageDimensions(CARD_WIDTH_LANDSCAPE, CARD_HEIGHT)

				Glide.with(viewHolder.view.context)
					.load(item.thumbnail)
					.centerCrop()
					.error(mDefaultCardImage)
					.into(cardView.mainImageView!!)
			}
		}
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
		Log.d(TAG, "onUnbindViewHolder")
		val cardView = viewHolder.view as ImageCardView
		// Remove references to images so that the garbage collector can free up memory
		cardView.badgeImage = null
		cardView.mainImage = null
	}

	private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
		val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
		// Both background colors should be set because the view"s background is temporarily visible
		// during animations.
		view.setBackgroundColor(color)
		view.setInfoAreaBackgroundColor(color)
	}

	companion object {
		private val TAG = "CardPresenter"

		private val CARD_WIDTH_LANDSCAPE = 313
		private val CARD_WIDTH_PORTRAIT = 117
		private val CARD_HEIGHT = 176
	}
}