package dev.kuylar.ownstream.tvleanback.ui.presenter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import dev.kuylar.ownstream.tvleanback.databinding.LayoutCardActionBinding

class ActionItemPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ActionCardViewHolder {
		val binding = LayoutCardActionBinding.inflate(LayoutInflater.from(parent.context))
		val vh = ActionCardViewHolder(binding)
		binding.root.isFocusable = true
		(binding.root as View).setOnFocusChangeListener { _, hasFocus ->
			vh.setSelected(hasFocus)
		}
		return ActionCardViewHolder(binding)
	}

	@Suppress("UNCHECKED_CAST")
	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		(item as? Pair<String, Int>)?.let { (text: String, icon: Int) ->
			(viewHolder as ActionCardViewHolder).bind(text, icon)
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {}

	class ActionCardViewHolder(private val binding: LayoutCardActionBinding) :
		ViewHolder(binding.root) {
		fun bind(item: String, icon: Int) {
			binding.contentTitle.text = item
			binding.image.setImageDrawable(ContextCompat.getDrawable(binding.root.context, icon))
		}

		fun setSelected(hasFocus: Boolean) {
			toggleMarquee(binding.contentTitle, hasFocus)
			toggleMarquee(binding.contentSubtitle, hasFocus)
		}

		private fun toggleMarquee(textView: TextView, enableMarquee: Boolean) {
			if (enableMarquee) {
				textView.apply {
					ellipsize = TextUtils.TruncateAt.MARQUEE
					isSingleLine = true
					marqueeRepeatLimit = -1
					isSelected = true
				}
			} else {
				textView.apply {
					ellipsize = TextUtils.TruncateAt.END
					isSingleLine = true
					isSelected = false
				}
			}
		}
	}
}