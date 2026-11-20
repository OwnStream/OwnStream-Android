package dev.kuylar.ownstream.tv.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.api.models.Season
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.ActivitySeasonBinding
import dev.kuylar.ownstream.tv.ui.adapter.EpisodeListAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class SeasonActivity : AppCompatActivity() {
	private lateinit var binding: ActivitySeasonBinding
	private lateinit var content: Content
	private lateinit var season: Season

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val contentJson = intent.extras?.getString("contentJson")
		val seasonJson = intent.extras?.getString("seasonJson")
		if (contentJson == null || seasonJson == null) {
			finish()
			return
		}
		content = Json.decodeFromString(contentJson)
		season = Json.decodeFromString<Season>(seasonJson)

		binding = ActivitySeasonBinding.inflate(layoutInflater)
		setContentView(binding.root)
		loadData(content, season)

		lifecycleScope.launch(Dispatchers.IO) {
			runCatching {
				client.getEpisodes(content.id, season.index).response!!
			}.onFailure { e ->
				if (e is CancellationException) return@onFailure
				withContext(Dispatchers.Main) {
					finish()
				}
				Log.e("SeasonActivity", "Failed to load season ${content.id}/${season.index}", e)
				//(activity as? MainActivity)?.onError(it)
				// TODO("Report errors")
			}.onSuccess {
				withContext(Dispatchers.Main) {
					loadData(it)
				}
			}
		}
	}

	private fun loadData(content: Content, season: Season) {
		binding.contentTitle.text = content.title
		binding.seasonTitle.text = season.translatedTitle ?: season.title ?: getString(
			R.string.content_season_template,
			season.index
		)
		if (season.description.isNullOrBlank())
			binding.seasonDesc.visibility = View.GONE
		else
			binding.seasonDesc.text = season.description
		binding.recycler.layoutManager = LinearLayoutManager(this)
		binding.recycler.adapter = EpisodeListAdapter(this)
		Glide.with(this).load(content.images.backdrop).into(binding.backdrop)
		Glide.with(this).load(season.poster ?: content.images.poster).into(binding.poster)
	}

	private fun loadData(episodes: List<Episode>) {
		(binding.recycler.adapter as EpisodeListAdapter).submitList(episodes)
		binding.recycler.post {
			binding.recycler.requestFocus()
		}
	}
}