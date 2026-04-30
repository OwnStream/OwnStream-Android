package dev.kuylar.ownstream.tvleanback.ui.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import dev.kuylar.ownstream.tvleanback.databinding.LayoutCardStandardBinding

open class MaterialCardView(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
	val binding = LayoutCardStandardBinding.inflate(LayoutInflater.from(context), this, true)

	val mainImageView = binding.image
	val textView = binding.contentTitle
	val contentView = binding.contentSubtitle

	constructor(context: Context) : this(context, null, 0)
	constructor(context: Context, attrs: AttributeSet) : this(
		context,
		attrs,
		0
	)

	init {
		clipChildren = false
		clipToPadding = false
		setFocusable(true);
		setFocusableInTouchMode(true);

		if (!isInEditMode) {
			init()
		}
	}

	private fun init() {
		binding.image.setOnFocusChangeListener { _, hasFocus ->
			setSelectedEffect(hasFocus)
		}
		setOnFocusChangeListener { _, hasFocus ->
			setSelectedEffect(hasFocus)
		}
	}

	var titleText: CharSequence
		get() = binding.contentTitle.text
		set(value) {
			binding.contentTitle.text = value
		}

	var contentText: CharSequence
		get() = binding.contentSubtitle.text
		set(value) {
			binding.contentSubtitle.text = value
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

	fun setMainImageDimensions(width: Int, height: Int) {
		val lp = binding.image.layoutParams
		@Suppress("RemoveRedundantQualifierName") // yeah no this has to be stated explicitly, or it wont work lol
		if (lp is android.view.ViewGroup.LayoutParams) {
			lp.width = width
			lp.height = height
		}
		binding.image.post {
			binding.image.layoutParams = lp
		}
	}

	fun setSelectedEffect(enabled: Boolean) {
		binding.image.isSelected = enabled
		toggleMarquee(binding.contentTitle, enabled)
		toggleMarquee(binding.contentSubtitle, enabled)
	}

	override fun setActivated(activated: Boolean) {
		super.setActivated(activated)
		binding.infoField.visibility = if (activated) VISIBLE else GONE
	}
}
