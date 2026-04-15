package dev.kuylar.ownstream.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.api.models.Episode
import dev.kuylar.ownstream.databinding.FragmentShowDetailsBinding
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class ShowDetailsFragment : Fragment() {
	private lateinit var binding: FragmentShowDetailsBinding
	private val contentId by lazy { arguments?.getString("contentId") }
	private val contentJson by lazy {
		try {
			Json.decodeFromString<Content>(arguments?.getString("contentJson") ?: "null")
		} catch (e: Exception) {
			Log.i(this.javaClass.name, "Failed to deserialize content from bundle", e)
			null
		}
	}

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentShowDetailsBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.description.text =
			contentJson?.translatedDescription ?: contentJson?.originalDescription
	}
}