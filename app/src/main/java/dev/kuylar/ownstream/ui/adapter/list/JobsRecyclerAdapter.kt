package dev.kuylar.ownstream.ui.adapter.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import dev.kuylar.ownstream.R
import dev.kuylar.ownstream.Utils
import dev.kuylar.ownstream.api.models.Job
import dev.kuylar.ownstream.databinding.ItemJobBinding
import java.time.Instant

class JobsRecyclerAdapter : RecyclerView.Adapter<JobsRecyclerAdapter.ViewHolder>() {
	private val list = SortedList(Job::class.java, object : SortedList.Callback<Job>() {
		override fun compare(o1: Job, o2: Job): Int {
			val statusPriority = mapOf(
				"Processing" to 1,
				"Starting" to 0,
				"Failed" to 2,
				"Pending" to 3,
				"Completed" to 4
			)

			val o1Priority = statusPriority[o1.status] ?: 5
			val o2Priority = statusPriority[o2.status] ?: 5

			if (o1Priority != o2Priority) {
				return o1Priority.compareTo(o2Priority)
			}

			val o1Time = parseJobTime(o1.updatedAt ?: o1.createdAt)
			val o2Time = parseJobTime(o2.updatedAt ?: o2.createdAt)

			return if (o1.status == "Pending") {
				o1Time.compareTo(o2Time)
			} else {
				o2Time.compareTo(o1Time)
			}
		}

		override fun onChanged(position: Int, count: Int) {
			notifyItemRangeChanged(position, count)
		}

		override fun areContentsTheSame(
			oldItem: Job?,
			newItem: Job?
		): Boolean {
			return oldItem == newItem
		}

		override fun areItemsTheSame(
			item1: Job?,
			item2: Job?
		): Boolean {
			return item1?.id == item2?.id
		}

		override fun onInserted(position: Int, count: Int) {
			notifyItemRangeInserted(position, count)
		}

		override fun onRemoved(position: Int, count: Int) {
			notifyItemRangeRemoved(position, count)
		}

		override fun onMoved(fromPosition: Int, toPosition: Int) {
			notifyItemMoved(fromPosition, toPosition)
		}

		private fun parseJobTime(value: String): Long {
			return runCatching {
				Instant.parse(value).toEpochMilli()
			}.getOrDefault(0L)
		}
	})

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	): ViewHolder {
		val binding = ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return ViewHolder(binding)
	}

	override fun onBindViewHolder(
		holder: ViewHolder,
		position: Int
	) {
		holder.bind(list[position])
	}

	override fun getItemCount(): Int {
		return list.size()
	}

	fun addJobs(jobs: List<Job>) {
		list.beginBatchedUpdates()
		jobs.forEach { job ->
			val existingIndex = (0 until list.size()).firstOrNull { list[it].id == job.id }
			if (existingIndex != null) {
				list.removeItemAt(existingIndex)
			}
			list.add(job)
		}
		list.endBatchedUpdates()
	}

	class ViewHolder(private val binding: ItemJobBinding) : RecyclerView.ViewHolder(binding.root) {
		fun bind(job: Job) {
			val (icon, state) = when (job.status) {
				"Pending" -> Pair(
					R.drawable.ic_job_pending,
					binding.root.context.getString(
						R.string.job_state_pending_template,
						Utils.toHhMmSsRelativeToNow(job.createdAt)
					)
				)

				"Starting" -> Pair(
					R.drawable.ic_job_starting,
					binding.root.context.getString(R.string.job_state_starting_template)
				)

				"Processing" -> Pair(
					R.drawable.ic_job_processing,
					binding.root.context.getString(
						R.string.job_state_processing_template,
						Utils.toHhMmSsRelativeToNow(job.startedAt ?: "1970-01-01T00:00:00Z")
					)
				)

				"Completed" -> Pair(
					R.drawable.ic_job_complete,
					binding.root.context.getString(
						R.string.job_state_completed_template,
						Utils.toHhMmSsRelativeToNow(job.completedAt ?: "1970-01-01T00:00:00Z"),
						Utils.toHhMmSsRelativeTo(
							job.startedAt ?: "1970-01-01T00:00:00Z",
							job.completedAt ?: "1970-01-01T00:00:00Z"
						)
					)
				)

				"Failed" -> Pair(
					R.drawable.ic_job_failed,
					binding.root.context.getString(
						R.string.job_state_failed_template,
						Utils.toHhMmSsRelativeToNow(job.completedAt ?: "1970-01-01T00:00:00Z"),
						Utils.toHhMmSsRelativeTo(
							job.startedAt ?: "1970-01-01T00:00:00Z",
							job.completedAt ?: "1970-01-01T00:00:00Z"
						)
					)
				)

				else -> Pair(
					R.drawable.ic_job_pending,
					binding.root.context.getString(R.string.job_state_starting_template)
				)
			}
			binding.title.text = job.jobType
			binding.message.text = job.message
			binding.state.text = state
			binding.icon.setImageResource(icon)

			if (job.status == "Completed" && job.progressMax == null) {
				binding.progress.isIndeterminate = false
				binding.progress.max = 1
				binding.progress.progress = 1
			} else {
				binding.progress.isIndeterminate = job.status == "Pending" || job.status == "Starting"
				binding.progress.max = job.progressMax ?: 100
				binding.progress.progress = job.progress ?: 0
			}
		}
	}
}