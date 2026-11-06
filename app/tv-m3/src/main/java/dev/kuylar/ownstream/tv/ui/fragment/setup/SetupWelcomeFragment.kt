package dev.kuylar.ownstream.tv.ui.fragment.setup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.api.store.OwnStreamServer
import dev.kuylar.ownstream.tv.R
import dev.kuylar.ownstream.tv.databinding.FragmentSetupWelcomeBinding
import dev.kuylar.ownstream.tv.ui.activity.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SetupWelcomeFragment : Fragment() {
	private lateinit var binding: FragmentSetupWelcomeBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentSetupWelcomeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		lifecycleScope.launch(Dispatchers.IO) {
			client.store.getServers().firstOrNull()?.let {
				withContext(Dispatchers.Main) {
					binding.field.editText!!.editableText.append(it.url)
				}
			}
		}
		binding.submit.setOnClickListener {
			lifecycleScope.launch(Dispatchers.IO) {
				val input = withContext(Dispatchers.Main) {
					binding.field.editText!!.text!!.toString()
				}
				val server = OwnStreamServer(
					UUID.randomUUID().toString(),
					input,
					input.toUri().authority ?: input
				)
				val existing = client.store.getServers().firstOrNull { it.url == input }
				if (existing == null) {
					client.store.addServer(server)
				}
				client.selectServer((existing ?: server).id)
				try {
					client.getInfo()
				} catch (e: Exception) {
					// not an os server
					client.store.removeServer(existing ?: server)
				}
				withContext(Dispatchers.Main) {
					(activity as? SetupActivity)?.openLoginFragment()
				}
			}
		}
	}
}