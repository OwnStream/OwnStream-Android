package dev.kuylar.ownstream.tv.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.api.models.EpisodeToWatchResponse
import dev.kuylar.ownstream.api.models.Season
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivityContentBinding
import dev.kuylar.ownstream.tv.ui.adapter.SeasonListAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class ContentActivity : AppCompatActivity() {
	private lateinit var binding: ActivityContentBinding
	private lateinit var id: String

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val _id = intent.extras?.getString("id")
		if (_id == null) {
			finish()
			return
		}
		id = _id

		binding = ActivityContentBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.watch.requestFocus()

		lifecycleScope.launch(Dispatchers.IO) {
			runCatching {
				Triple(
					client.getContentDetails(id).response!!,
					client.getSeasons(id).response ?: emptyList(),
					client.getEpisodeToWatch(id).response
				)
			}.onFailure { e ->
				if (e is CancellationException) return@onFailure
				withContext(Dispatchers.Main) {
					finish()
				}
				// TODO("Report errors")
				//(activity as? MainActivity)?.onError(it)
			}.onSuccess {
				withContext(Dispatchers.Main) {
					loadData(it.first, it.second, it.third)
				}
			}
		}
	}

	@SuppressLint("SetTextI18n")
	private fun loadData(
		content: Content,
		seasons: List<Season>,
		toWatch: EpisodeToWatchResponse?
	) {
		binding.contentTitle.text = content.title
		if (content.tagline.isNotBlank())
			binding.contentTagline.text = content.tagline
		else
			binding.contentTagline.visibility = View.GONE
		binding.contentDescription.text = content.description

		// TODO: from settings, fallback to one from server
		content.ageRatings["TR"].let {
			binding.rating.isVisible = it != null
			binding.rating.text = it
		}
		binding.year.text = content.releasedAt.take(4)
		val parts = content.runtime.split(":")
		val h = parts[0].toInt()
		val m = parts[1].toInt()
		binding.runtime.text = getString(R.string.content_runtime, h, m)

		binding.contentDescription.text = content.description

		val episode = toWatch?.continueWatching ?: toWatch?.upNext
		val video = episode?.videos?.firstOrNull()
		if (toWatch != null && episode != null && video != null) {
			val isContinue = toWatch.continueWatching != null
			val text = if (content.type == "Tv") {
				getString(
					if (isContinue) R.string.content_tv_continue else R.string.content_tv_watch,
					episode.seasonNumber,
					episode.episodeNumber
				)
			} else {
				getString(if (isContinue) R.string.content_movie_continue else R.string.content_movie_watch)
			}
			binding.watch.setText(text)
			binding.watch.setOnClickListener {
				startActivity(Intent(this, PlayerActivity::class.java).apply {
					putExtra("videoId", video.id)
				})
			}
		} else binding.watch.setText(getString(R.string.content_video_unavailable))

		if (content.type == "Movie" && video != null) {
			binding.contentExtraDetailsQuality.text = "${video.width}x${video.height}"
			binding.contentExtraDetailsDub.text = video.language
			binding.details.isVisible = true
		} else {
			binding.seasonsRecycler.layoutManager =
				LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
			binding.seasonsRecycler.adapter = SeasonListAdapter(this, Json.encodeToString(content))
				.apply { submitList(seasons) }
			binding.seasons.isVisible = true
		}

		Glide.with(this).load(content.images.backdrop).into(binding.backdrop)
		binding.watch.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
			if (hasFocus) binding.scroll.smoothScrollTo(0, 0)
		}
	}
}