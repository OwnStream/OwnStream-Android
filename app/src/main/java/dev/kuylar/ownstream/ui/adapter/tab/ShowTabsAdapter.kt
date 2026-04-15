package dev.kuylar.ownstream.ui.adapter.tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.kuylar.ownstream.api.models.Content
import dev.kuylar.ownstream.ui.fragment.ShowDetailsFragment
import dev.kuylar.ownstream.ui.fragment.ShowEpisodesFragment
import kotlinx.serialization.json.Json

class ShowTabsAdapter(val fragment: Fragment, val content: Content) :
	FragmentStateAdapter(fragment) {
	private val cache = mutableMapOf<Int, Fragment>()

	override fun createFragment(position: Int): Fragment {
		if (!cache.containsKey(position)) {
			val f = when (position) {
				0 -> ShowDetailsFragment()
				else -> ShowEpisodesFragment()
			}
			f.arguments = Bundle().apply {
				putString("contentId", content.id)
				if (position == 0) putString("contentJson", Json.encodeToString(content))
			}
			cache[position] = f
		}
		return cache[position]!!
	}

	override fun getItemCount() = 2
}