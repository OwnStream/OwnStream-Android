package dev.kuylar.ownstream.tv.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.api.models.EpisodeToWatchResponse
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivityContentBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
				Pair(
					client.getContentDetails(id).response!!,
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
					loadData(it.first, it.second)
				}
			}
		}
	}

	@SuppressLint("SetTextI18n")
	private fun loadData(content: Content, toWatch: EpisodeToWatchResponse?) {
		binding.contentTitle.text = content.title
		binding.contentTagline.text = content.tagline
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
				Toast.makeText(this, "Play: ${video.id}", Toast.LENGTH_LONG).show()
			}
		} else binding.watch.setText(getString(R.string.content_video_unavailable))

		if (content.type == "Movie" && video != null) {
			binding.contentExtraDetailsQuality.text = "${video.width}x${video.height}"
			binding.contentExtraDetailsDub.text = video.language
		} else {
			binding.details.isVisible = false
		}

		Glide.with(this).load(content.images.backdrop).into(binding.backdrop)
	}
}