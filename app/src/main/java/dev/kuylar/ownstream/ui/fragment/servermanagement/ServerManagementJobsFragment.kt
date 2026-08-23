package dev.kuylar.ownstream.ui.fragment.servermanagement

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.api.OwnStreamApiClient
import dev.kuylar.ownstream.databinding.FragmentServerManagementJobsBinding
import dev.kuylar.ownstream.ui.adapter.list.JobsRecyclerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ServerManagementJobsFragment : Fragment() {
	private lateinit var binding: FragmentServerManagementJobsBinding
	private lateinit var adapter: JobsRecyclerAdapter
	private var progressUpdateJob: Job? = null
	private var currentPage = 0
	private var isLoading = false

	@Inject
	lateinit var client: OwnStreamApiClient

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentServerManagementJobsBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		adapter = JobsRecyclerAdapter()
		binding.root.adapter = adapter
		val layoutManager = LinearLayoutManager(requireContext())
		binding.root.layoutManager = layoutManager

		binding.root.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)

				val visibleItemCount = layoutManager.childCount
				val totalItemCount = layoutManager.itemCount
				val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

				if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
					currentPage++
					lifecycleScope.launch(Dispatchers.IO) {
						loadJobsPage(currentPage)
					}
				}
			}
		})

		startJobRefresh()
	}

	private suspend fun loadJobsPage(page: Int) {
		if (isLoading) return
		isLoading = true

		val jobs = client.getJobs(0, page).response?.items ?: emptyList()
		withContext(Dispatchers.Main) {
			adapter.addJobs(jobs)
			isLoading = false
		}
	}

	private fun startJobRefresh() {
		progressUpdateJob?.cancel()
		progressUpdateJob = lifecycleScope.launch(Dispatchers.IO) {
			var delta = System.currentTimeMillis()
			loadJobsPage(0)
			while (isActive) {
				delay(5.seconds)
				val newJobs = client.getJobs(delta).response?.items ?: emptyList()
				delta = System.currentTimeMillis()
				withContext(Dispatchers.Main) {
					adapter.addJobs(newJobs)
				}
			}
		}
	}
}