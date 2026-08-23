package dev.kuylar.ownstream.ui.fragment.servermanagement

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import dev.kuylar.ownstream.R

class ServerManagementItemFragment : PreferenceFragmentCompat() {
	var onCategorySelected: ((String) -> Unit)? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.settings_categories, rootKey)
		preferenceScreen.forEach { preference ->
			preference.setOnPreferenceClickListener {
				onCategorySelected?.invoke(it.key)
				true
			}
		}
	}
}
