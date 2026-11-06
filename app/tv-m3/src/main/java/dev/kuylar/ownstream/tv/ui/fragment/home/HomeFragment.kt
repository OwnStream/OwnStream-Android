package dev.kuylar.ownstream.tv.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.leanbackm3views.card.Card
import dev.kuylar.leanbackm3views.shelf.BaseShelfFragment
import dev.kuylar.leanbackm3views.shelf.Shelf
import dev.kuylar.leanbackm3views.shelf.ShelfType
import dev.kuylar.leanbackm3views.util.dp
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.ui.activity.ContentActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class HomeFragment : BaseShelfFragment() {
	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		recyclerView.post {
			recyclerView.setPadding(
				recyclerView.paddingLeft,
				recyclerView.paddingTop + requireContext().dp(72f).roundToInt(),
				recyclerView.paddingRight,
				recyclerView.paddingBottom + requireContext().dp(32f).roundToInt()
			)
			// TODO: Somehow make it so only the initial load opens this
			// recyclerView.scrollToPosition(0)
		}
	}

	override fun onResume() {
		super.onResume()
		lifecycleScope.launch(Dispatchers.Main) {
			val shelves = runCatching {
				withContext(Dispatchers.IO) {
					listOfNotNull(
						getShelf("nextUp", getString(R.string.shelf_nextup)),
						getShelf("continue", getString(R.string.shelf_continue)),
						getShelf("recent", getString(R.string.shelf_recents)),
						getShelf("recentContent/movie", getString(R.string.shelf_movies)),
						getShelf("recentContent/tv", getString(R.string.shelf_series)),
					)
				}
			}.onFailure { e ->
				if (e is CancellationException) return@onFailure
				TODO("Report errors")
				//(activity as? MainActivity)?.onError(it)
			}.getOrElse {
				emptyList()
			}
			Log.i("HomeFragment", "Loaded ${shelves.size} shelves")
			setShelves(shelves)
		}
	}

	private suspend fun getShelf(id: String, title: String): Shelf? {
		try {
			val items = client.getHomeShelf(id).response ?: emptyList();
			if (items.isEmpty())
				return null

			return Shelf(title, null, ShelfType.Normal, items.map {
				Card(
					title = it.title,
					subtitle = it.subtitle.joinToString(" • "),
					image = it.image,
					width = if (it.type == "episode" || it.type == "video") 320 else 120,
					height = 180,
					onClick = {
						if (it.type == "episode" || it.type == "video") {
							Toast.makeText(
								requireContext(),
								"Play: ${it.videoId}",
								Toast.LENGTH_SHORT
							).show()
						} else {
							startActivity(
								Intent(
									requireContext(),
									ContentActivity::class.java
								).apply {
									putExtra("id", it.id)
								})
						}
					}
				)
			}, null)
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load shelf: $id", e)
			return null
		}
	}
}