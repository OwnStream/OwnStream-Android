package dev.kuylar.ownstream.tv.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.leanbackm3views.card.Card
import dev.kuylar.leanbackm3views.grid.BaseGridFragment
import dev.kuylar.leanbackm3views.util.dp
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.tv.ui.activity.ContentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class LibraryFragment : BaseGridFragment() {
	private lateinit var typeFilter: String
	override val spanCount: Int
		get() = 6

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.let {
			typeFilter = it.getString("type") ?: "movie"
		}
	}

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
			val items = runCatching {
				withContext(Dispatchers.IO) {
					client.getLibraryItems(null, typeFilter).response
				}
			}.onFailure {
				TODO("Report errors")
				//(activity as? MainActivity)?.onError(it)
			}.getOrElse {
				null
			}
			// TODO: load more
			items?.items?.let { list ->
				setCards(
					list
					// should be server-side honestly
					.sortedBy { it.translatedTitle ?: it.originalTitle }
					.map {
						Card(
							title = it.translatedTitle ?: it.originalTitle,
							subtitle = it.releasedAt.take(4),
							image = it.images.poster,
							backgroundImage = it.images.backdrop,
							width = 120,
							height = 180,
							onClick = {
								startActivity(
									Intent(
										requireContext(),
										ContentActivity::class.java
									).apply {
										putExtra("id", it.id)
									})
							}
						)
					})
			}
		}
	}
}