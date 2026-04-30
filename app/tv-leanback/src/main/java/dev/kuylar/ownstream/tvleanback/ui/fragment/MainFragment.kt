package dev.kuylar.ownstream.tvleanback.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.core.view.postDelayed
import androidx.leanback.widget.FocusHighlight
import androidx.lifecycle.lifecycleScope

import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.ShelfItem
import dev.kuylar.ownstream.tvleanback.ui.activity.DetailsActivity
import dev.kuylar.ownstream.tvleanback.ui.activity.MainActivity
import dev.kuylar.ownstream.tvleanback.ui.activity.PlaybackActivity
import dev.kuylar.ownstream.tvleanback.R
import dev.kuylar.ownstream.tvleanback.ui.presenter.CardPresenter
import dev.kuylar.ownstream.tvleanback.ui.presenter.ActionItemPresenter
import dev.kuylar.ownstream.tvleanback.ui.view.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : BrowseSupportFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient

	@Deprecated("Deprecated in Java")
	override fun onActivityCreated(savedInstanceState: Bundle?) {
		@Suppress("DEPRECATION")
		super.onActivityCreated(savedInstanceState)

		setupUIElements()
		loadRows()
	}

	private fun setupUIElements() {
		title = "OwnStream"
		// over title
		headersState = HEADERS_DISABLED
		isHeadersTransitionOnBackEnabled = false
		// set search icon color
		searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
		onItemViewClickedListener = ItemViewClickedListener()
	}

	private fun loadRows() {
		val presenter = ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
			selectEffectEnabled = false
			shadowEnabled = false
		}
		val rowsAdapter = ArrayObjectAdapter(presenter)
		val cardPresenter = CardPresenter()

		val gridHeader = HeaderItem(-1L, "PREFERENCES")

		val mGridPresenter = ActionItemPresenter()
		val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
		gridRowAdapter.add(Pair(getString(R.string.grid_view), R.drawable.ic_error))
		gridRowAdapter.add(Pair(getString(R.string.error_fragment), R.drawable.ic_error))
		gridRowAdapter.add(Pair(getString(R.string.personal_settings), R.drawable.ic_error))
		rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

		adapter = rowsAdapter

		lifecycleScope.launch {
			val shelves = runCatching {
				withContext(Dispatchers.IO) {
					client.getHomeShelves().response!!
				}
			}.onFailure {
				(activity as? MainActivity)?.onError(it)
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
			view?.postDelayed(50) {
				setSelectedPosition(0, true)
				view?.postDelayed(500) {
					setupEventListeners()
				}
			}
		}
	}

	private fun setupEventListeners() {
		setOnSearchClickedListener {
			Toast.makeText(requireActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
				.show()
		}
	}

	private inner class ItemViewClickedListener : OnItemViewClickedListener {
		override fun onItemClicked(
			itemViewHolder: Presenter.ViewHolder,
			item: Any,
			rowViewHolder: RowPresenter.ViewHolder,
			row: Row
		) {

			if (item is ShelfItem) {
				when (item.type) {
					"video", "episode" -> {
						val intent = if (item.videoId != null) {
							val intent = Intent(requireActivity(), PlaybackActivity::class.java)
							intent.putExtra("videoId", item.videoId)
							intent
						} else {
							val intent = Intent(requireActivity(), DetailsActivity::class.java)
							intent.putExtra(DetailsActivity.Companion.MOVIE, item.id)
							intent
						}
						startActivity(intent)
					}

					else -> {
						val intent = Intent(requireActivity(), DetailsActivity::class.java)
						intent.putExtra(DetailsActivity.Companion.MOVIE, item.id)

						val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
							requireActivity(),
							(itemViewHolder.view as MaterialCardView).mainImageView!!,
							DetailsActivity.Companion.SHARED_ELEMENT_NAME
						).toBundle()
						startActivity(intent, bundle)
					}
				}
			} else if (item is Pair<*, *> && item.first is String) {
				if ((item.first as String).contains(getString(R.string.error_fragment))) {
					(activity as? MainActivity)?.onError(Exception("Test exception"))
				} else {
					Toast.makeText(requireActivity(), (item.first as String), Toast.LENGTH_SHORT)
						.show()
				}
			}
		}
	}
}
