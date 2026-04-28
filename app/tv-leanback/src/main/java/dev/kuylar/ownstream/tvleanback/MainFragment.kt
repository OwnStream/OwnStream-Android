package dev.kuylar.ownstream.tvleanback

import java.util.Collections
import java.util.Timer
import java.util.TimerTask

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Shelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : BrowseSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient

	private val mHandler = Handler(Looper.myLooper()!!)
	private lateinit var mBackgroundManager: BackgroundManager
	private var mDefaultBackground: Drawable? = null
	private lateinit var mMetrics: DisplayMetrics
	private var mBackgroundTimer: Timer? = null
	private var mBackgroundUri: String? = null

	@Deprecated("Deprecated in Java")
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		Log.i(TAG, "onCreate")
		@Suppress("DEPRECATION")
		super.onActivityCreated(savedInstanceState)

		prepareBackgroundManager()
		setupUIElements()
		loadRows()
		setupEventListeners()
	}

	override fun onDestroy() {
		super.onDestroy()
		Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
		mBackgroundTimer?.cancel()
	}

	private fun prepareBackgroundManager() {
		mBackgroundManager = BackgroundManager.getInstance(activity)
		mBackgroundManager.attach(requireActivity().window)
		mDefaultBackground =
			ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
		mMetrics = DisplayMetrics()
		requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
	}

	private fun setupUIElements() {
		title = "OwnStream"
		// over title
		headersState = BrowseSupportFragment.HEADERS_ENABLED
		isHeadersTransitionOnBackEnabled = true

		// set fastLane (or headers) background color
		brandColor = ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
		// set search icon color
		searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
	}

	private fun loadRows() {
		val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
		val cardPresenter = CardPresenter()

		val gridHeader = HeaderItem(NUM_ROWS.toLong(), "PREFERENCES")

		val mGridPresenter = GridItemPresenter()
		val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
		gridRowAdapter.add(resources.getString(R.string.grid_view))
		gridRowAdapter.add(getString(R.string.error_fragment))
		gridRowAdapter.add(resources.getString(R.string.personal_settings))
		rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

		adapter = rowsAdapter

		lifecycleScope.launch {
			val shelves = runCatching {
				withContext(Dispatchers.IO) {
					client.getHomeShelves().response!!
				}
			}.onFailure {
				Toast.makeText(
					requireContext(),
					getString(R.string.error_shelves),
					Toast.LENGTH_LONG
				).show()
			}.getOrElse {
				emptyList()
			}

			shelves.forEachIndexed { index, shelf ->
				val listRowAdapter = ArrayObjectAdapter(cardPresenter)
				listRowAdapter.addAll(0, shelf.items)
				val header = HeaderItem(shelf.type.hashCode().toLong(), shelf.title)
				rowsAdapter.add(index, ListRow(header, listRowAdapter))
			}
			rowsAdapter.notifyItemRangeChanged(0, shelves.size + 1)
			setSelectedPosition(0, false)
		}
	}

	private fun setupEventListeners() {
		setOnSearchClickedListener {
			Toast.makeText(requireActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
				.show()
		}

		onItemViewClickedListener = ItemViewClickedListener()
		onItemViewSelectedListener = ItemViewSelectedListener()
	}

	private inner class ItemViewClickedListener : OnItemViewClickedListener {
		override fun onItemClicked(
			itemViewHolder: Presenter.ViewHolder,
			item: Any,
			rowViewHolder: RowPresenter.ViewHolder,
			row: Row
		) {

			if (item is Movie) {
				Log.d(TAG, "Item: " + item.toString())
				val intent = Intent(requireActivity(), DetailsActivity::class.java)
				intent.putExtra(DetailsActivity.MOVIE, item)

				val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
					requireActivity(),
					(itemViewHolder.view as ImageCardView).mainImageView!!,
					DetailsActivity.SHARED_ELEMENT_NAME
				)
					.toBundle()
				startActivity(intent, bundle)
			} else if (item is String) {
				if (item.contains(getString(R.string.error_fragment))) {
					val intent = Intent(requireActivity(), BrowseErrorActivity::class.java)
					startActivity(intent)
				} else {
					Toast.makeText(requireActivity(), item, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
		override fun onItemSelected(
			itemViewHolder: Presenter.ViewHolder?, item: Any?,
			rowViewHolder: RowPresenter.ViewHolder, row: Row
		) {
			if (item is Movie) {
				mBackgroundUri = item.backgroundImageUrl
				startBackgroundTimer()
			}
		}
	}

	private fun updateBackground(uri: String?) {
		val width = mMetrics.widthPixels
		val height = mMetrics.heightPixels
		Glide.with(requireActivity())
			.load(uri)
			.centerCrop()
			.error(mDefaultBackground)
			.into<SimpleTarget<Drawable>>(
				object : SimpleTarget<Drawable>(width, height) {
					override fun onResourceReady(
						drawable: Drawable,
						transition: Transition<in Drawable>?
					) {
						mBackgroundManager.drawable = drawable
					}
				})
		mBackgroundTimer?.cancel()
	}

	private fun startBackgroundTimer() {
		mBackgroundTimer?.cancel()
		mBackgroundTimer = Timer()
		mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
	}

	private inner class UpdateBackgroundTask : TimerTask() {

		override fun run() {
			mHandler.post { updateBackground(mBackgroundUri) }
		}
	}

	private inner class GridItemPresenter : Presenter() {
		override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
			val view = TextView(parent.context)
			view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
			view.isFocusable = true
			view.isFocusableInTouchMode = true
			view.setBackgroundColor(
				ContextCompat.getColor(
					requireActivity(),
					R.color.default_background
				)
			)
			view.setTextColor(Color.WHITE)
			view.gravity = Gravity.CENTER
			return Presenter.ViewHolder(view)
		}

		override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
			(viewHolder.view as TextView).text = item as String
		}

		override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
	}

	companion object {
		private val TAG = "MainFragment"

		private val BACKGROUND_UPDATE_DELAY = 300
		private val GRID_ITEM_WIDTH = 200
		private val GRID_ITEM_HEIGHT = 200
		private val NUM_ROWS = 6
		private val NUM_COLS = 15
	}
}
