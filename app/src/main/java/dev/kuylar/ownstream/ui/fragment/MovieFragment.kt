package dev.kuylar.ownstream.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils.visibleIf
import dev.kuylar.ownstream.Utils.visibleIfNotBlank
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.FragmentMovieBinding
import dev.kuylar.ownstream.ui.activity.PlayerActivity
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MovieFragment : Fragment() {
	private lateinit var binding: FragmentMovieBinding
	private lateinit var id: String

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.getString("id")?.let { id = it }
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentMovieBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				refresh()
			}
		}
		binding.srl.setOnRefreshListener {
			lifecycleScope.launch {
				refresh()
			}
		}
	}

	private suspend fun refresh() {
		binding.srl.isRefreshing = true
		val movie = try {
			val details = client.getContentDetails(id)
			details.takeIf { it.response?.type == "Movie" }
				?: throw Exception("Unexpected content type $id. Expected 'Movie', got '${details.response?.type}'.")
		} catch (e: Exception) {
			Log.e(this.javaClass.name, "Failed to load movie", e)
			null
		}?.response
		binding.srl.isRefreshing = false

		if (movie == null) {
			findNavController().popBackStack()
			return
		}

		Glide.with(this).load(movie.images.backdrop).into(binding.backdrop)
		Glide.with(this).load(movie.images.poster).into(binding.poster)

		val originalTitle = movie.originalTitle.takeIf { it.isNotEmpty() }
		val translatedTitle = movie.translatedTitle?.takeIf { it.isNotEmpty() }

		when {
			originalTitle != null && translatedTitle != null
					&& !originalTitle.equals(translatedTitle, ignoreCase = true) -> {
				binding.title.text = translatedTitle
				binding.originalTitle.text = originalTitle
				binding.originalTitle.visibility = View.VISIBLE
			}

			translatedTitle != null -> {
				binding.title.text = translatedTitle
			}

			originalTitle != null -> {
				binding.title.text = originalTitle
			}
		}

		binding.tagline.text = movie.translatedTagline ?: movie.originalTagline
		binding.description.text = movie.translatedDescription ?: movie.originalDescription
		binding.originalTitle.visibleIfNotBlank()
		binding.tagline.visibleIfNotBlank()

		binding.buttonWatch.setOnClickListener {
			lifecycleScope.launch {
				binding.buttonWatch.isEnabled = false
				val episodes = try {
					client.getEpisodes(id, 1).response
				} catch (e: Exception) {
					Log.e(this.javaClass.name, "Failed to get episode ID", e)
					null
				}
				binding.buttonWatch.isEnabled = true

				val episode = episodes?.firstOrNull()
				val videoId = episode?.videos?.firstOrNull()?.id

				if (videoId == null) {
					Toast.makeText(
						requireContext(),
						R.string.error_video_not_found,
						Toast.LENGTH_LONG
					).show()
				}

				val intent = Intent(requireContext(), PlayerActivity::class.java)
				intent.putExtra("episode", episode?.id)
				intent.putExtra("video", videoId)
				startActivity(intent)
			}
		}
	}
}