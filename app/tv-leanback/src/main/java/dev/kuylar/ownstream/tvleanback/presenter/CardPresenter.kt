package dev.kuylar.ownstream.tvleanback.presenter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.ShelfItem
import dev.kuylar.ownstream.tvleanback.R
import dev.kuylar.ownstream.tvleanback.view.MaterialCardView
import kotlin.properties.Delegates

class CardPresenter : Presenter() {
	private var mDefaultCardImage: Drawable? = null

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		Log.d(TAG, "onCreateViewHolder")

		mDefaultCardImage =
			ContextCompat.getDrawable(parent.context, R.drawable.default_movie_thumbnail)

		val cardView = MaterialCardView(parent.context)

		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true
		return ViewHolder(cardView)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val cardView = viewHolder.view as MaterialCardView
		when (item) {
			is ShelfItem -> {
				Log.d(TAG, "onBindViewHolder")
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
					.into(cardView.mainImageView)
			}

			is Episode -> {
				cardView.titleText =
					viewHolder.view.context.getString(
						R.string.episode_title_template,
						item.episodeNumber,
						item.translatedTitle
					)
				cardView.contentText =
					viewHolder.view.context.getString(
						R.string.episode_content_template,
						item.runtime
					)
				cardView.setMainImageDimensions(CARD_WIDTH_LANDSCAPE, CARD_HEIGHT)

				Glide.with(viewHolder.view.context)
					.load(item.thumbnail)
					.centerCrop()
					.error(mDefaultCardImage)
					.into(cardView.mainImageView)
			}
		}
	}

	override fun onUnbindViewHolder(p0: ViewHolder) {
		// uhhhhhhhhhhhhhhhhhh
	}

	companion object {
		private const val TAG = "CardPresenter"

		private const val CARD_WIDTH_LANDSCAPE = 313
		private const val CARD_WIDTH_PORTRAIT = 117
		private const val CARD_HEIGHT = 176
	}
}