package dev.kuylar.ownstream.tvleanback

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.graphics.drawable.Drawable
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast
import androidx.leanback.widget.FocusHighlight
import androidx.lifecycle.lifecycleScope

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.Season
import dev.kuylar.ownstream.tvleanback.view.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class VideoDetailsFragment : DetailsSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient

	private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
	private lateinit var mPresenterSelector: ClassPresenterSelector
	private lateinit var mAdapter: ArrayObjectAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		Log.d(TAG, "onCreate DetailsFragment")
		super.onCreate(savedInstanceState)

		mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

		val contentId = requireActivity().intent.getStringExtra(DetailsActivity.MOVIE)
		if (contentId != null) {
			loadContent(contentId)
		} else {
			val intent = Intent(requireActivity(), MainActivity::class.java)
			startActivity(intent)
		}
	}

	private fun loadContent(id: String) {
		mPresenterSelector = ClassPresenterSelector()
		mAdapter = ArrayObjectAdapter(mPresenterSelector)
		lifecycleScope.launch {
			runCatching {
				withContext(Dispatchers.IO) {
					Triple(
						client.getContentDetails(id).response!!,
						client.getEpisodeToWatch(id).response!!,
						client.getSeasons(id).response!!
					)
				}
			}.onFailure {
				(activity as? DetailsActivity)?.onError(it)
			}.onSuccess {
				val (content, upNext, seasons) = it
				val isContinue = upNext.continueWatching != null
				val episode = upNext.continueWatching ?: upNext.upNext
				val videoId = episode?.videos?.firstOrNull()?.id
				setupDetailsOverviewRow(content, isContinue, episode, videoId)
				setupDetailsOverviewRowPresenter(videoId)
				if (content.type == "Tv") {
					setupSeasonRows(content.id, seasons)
				}
				adapter = mAdapter
				initializeBackground(content)
				onItemViewClickedListener = ItemViewClickedListener()
			}
		}
	}

	private fun initializeBackground(content: Content) {
		if (content.images.backdrop == null) return
		mDetailsBackground.enableParallax()
		Glide.with(requireActivity())
			.asBitmap()
			.centerCrop()
			.error(R.drawable.default_background)
			.load(content.images.backdrop)
			.into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
				override fun onResourceReady(
					bitmap: Bitmap,
					transition: Transition<in Bitmap>?
				) {
					mDetailsBackground.coverBitmap = bitmap
					mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
				}
			})
	}

	private fun setupDetailsOverviewRow(
		content: Content,
		isContinue: Boolean,
		episode: Episode?,
		videoId: String?
	) {
		val row = DetailsOverviewRow(content)
		row.imageDrawable =
			ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
		val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
		val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)
		Glide.with(requireActivity())
			.load(content.images.poster)
			.fitCenter()
			.error(R.drawable.default_background)
			.into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
				override fun onResourceReady(
					drawable: Drawable,
					transition: Transition<in Drawable>?
				) {
					row.imageDrawable = drawable
					mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
				}
			})


		val actionAdapter = ArrayObjectAdapter()

		if (episode != null && videoId != null) {
			actionAdapter.add(
				Action(
					ACTION_WATCH,
					getString(if (isContinue) R.string.watch_continue else R.string.watch),
					if (content.type == "Tv") getString(
						R.string.watch_episode_template,
						episode.seasonNumber,
						episode.episodeNumber
					) else null
				)
			)
		} else if (episode != null) {
			actionAdapter.add(
				Action(
					ACTION_ERROR,
					getString(R.string.watch_unavailable)
				)
			)
		}
		row.actionsAdapter = actionAdapter

		mAdapter.add(row)
	}

	private fun setupDetailsOverviewRowPresenter(videoId: String?) {
		// Set detail background.
		val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
		detailsPresenter.backgroundColor =
			ContextCompat.getColor(requireActivity(), R.color.selected_background)

		// Hook up transition element.
		val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
		sharedElementHelper.setSharedElementEnterTransition(
			activity, DetailsActivity.SHARED_ELEMENT_NAME
		)
		detailsPresenter.setListener(sharedElementHelper)
		detailsPresenter.isParticipatingEntranceTransition = true

		if (videoId != null)
			detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
				when (action.id) {
					ACTION_WATCH -> {
						val intent = Intent(requireActivity(), PlaybackActivity::class.java)
						intent.putExtra("videoId", videoId)
						startActivity(intent)
					}

					else -> {
						Toast.makeText(requireActivity(), action.toString(), Toast.LENGTH_SHORT)
							.show()
					}
				}
			}
		mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
	}

	private fun setupSeasonRows(id: String, seasons: List<Season>) {
		val presenter = ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
			selectEffectEnabled = false
			shadowEnabled = false
		}
		lifecycleScope.launch {
			val allSeasons = withContext(Dispatchers.IO) {
				seasons.map { s ->
					runCatching {
						Pair(s, client.getEpisodes(id, s.index).response!!)
					}.onFailure {
						Log.e("VideoDetailsFragment", "Failed to load season ${id}/${s.index}", it)
					}.getOrDefault(Pair(s, emptyList()))
				}.associate { it }
			}

			allSeasons.forEach { (season, episodes) ->
				val listRowAdapter = ArrayObjectAdapter(CardPresenter())

				listRowAdapter.addAll(0, episodes)

				val header = HeaderItem(0, getString(R.string.season_title_template, season.index))
				mAdapter.add(ListRow(header, listRowAdapter))
				mPresenterSelector.addClassPresenter(ListRow::class.java, presenter)
			}
		}
	}

	private fun convertDpToPixel(context: Context, dp: Int): Int {
		val density = context.applicationContext.resources.displayMetrics.density
		return (dp.toFloat() * density).roundToInt()
	}

	private inner class ItemViewClickedListener() :
		OnItemViewClickedListener {
		override fun onItemClicked(
			itemViewHolder: Presenter.ViewHolder,
			item: Any?,
			rowViewHolder: RowPresenter.ViewHolder,
			row: Row
		) {
			when (item) {
				is Content -> {
					val intent = Intent(requireActivity(), DetailsActivity::class.java)
					intent.putExtra(DetailsActivity.MOVIE, item.id)

					val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
						requireActivity(),
						(itemViewHolder.view as MaterialCardView).mainImageView,
						DetailsActivity.SHARED_ELEMENT_NAME
					).toBundle()
					startActivity(intent, bundle)
				}

				is Episode -> {
					item.videos.firstOrNull()?.let { video ->
						val intent = Intent(requireActivity(), PlaybackActivity::class.java)
						intent.putExtra("videoId", video.id)
						startActivity(intent)
					}
				}
			}
		}
	}

	companion object {
		private const val TAG = "VideoDetailsFragment"

		private const val ACTION_WATCH = 1L
		private const val ACTION_ERROR = 0L

		private const val DETAIL_THUMB_WIDTH = 274
		private const val DETAIL_THUMB_HEIGHT = 411
	}
}