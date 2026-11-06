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
import dev.kuylar.ownstream.tv.databinding.FragmentSetupServerSelectBinding
import dev.kuylar.ownstream.tv.databinding.ItemServerBinding
import dev.kuylar.ownstream.tv.ui.activity.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SetupServerSelectFragment : Fragment() {
	private lateinit var binding: FragmentSetupServerSelectBinding

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentSetupServerSelectBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		lifecycleScope.launch(Dispatchers.IO) {
			client.store.getServers().forEach {
				withContext(Dispatchers.Main) {
					val itemBinding = ItemServerBinding.inflate(layoutInflater, binding.serverList, false)
					itemBinding.root.setTitle(it.name)
					itemBinding.root.setSubtitle(it.url)
					itemBinding.root.setOnClickListener { _ ->
						lifecycleScope.launch(Dispatchers.IO) {
							client.selectServer(it.id)
							withContext(Dispatchers.Main) {
								(activity as? SetupActivity)?.openLoginFragment()
							}
						}
					}
					binding.serverList.addView(itemBinding.root)
				}
			}
		}
		binding.addServer.setOnClickListener {
			(activity as? SetupActivity)?.openServerFragment()
		}
	}
}