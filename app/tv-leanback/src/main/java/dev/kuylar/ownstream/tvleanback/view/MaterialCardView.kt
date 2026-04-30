package dev.kuylar.ownstream.tvleanback.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import dev.kuylar.ownstream.tvleanback.databinding.LayoutCardStandardBinding
import kotlin.math.roundToInt

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

		/*
		binding.image.setOnKeyListener { v, keyCode, event ->
			if (keyCode == KeyEvent.KEYCODE_ENTER ||
				keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
				keyCode == KeyEvent.KEYCODE_DPAD_CENTER
			) {
				when (event.action) {
					KeyEvent.ACTION_DOWN -> {
						animateImageScale(0.95f)
					}

					KeyEvent.ACTION_UP -> {
						if (v.isFocused || v.hasFocus())
							animateImageScale(1.1f)
						else
							animateImageScale(1.0f)
					}
				}
			}
			false
		}

		binding.image.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
			if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
				val width = right - left
				val height = bottom - top

				val marginX = (0.05 * width).toInt()
				val marginY = (0.05 * height).toInt()

				val lp = v.layoutParams
				if (lp is MarginLayoutParams) {
					lp.setMargins(
						marginX,
						marginY,
						marginX,
						marginY
					)
					v.post {
						v.setLayoutParams(lp)
					}
				}
			}
		}
		 */
	}

	private fun animateImageScale(scale: Float) {
		return
		// decided to use the default card zoom
		// so i dont mess with margins & stuff
		/*
		binding.image.animate()
			.scaleX(scale)
			.scaleY(scale)
			.setDuration(150)
			.start()
		 */
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
		animateImageScale(if (enabled) 1.1f else 1.0f)
		toggleMarquee(binding.contentTitle, enabled)
		toggleMarquee(binding.contentSubtitle, enabled)
	}

	override fun setActivated(activated: Boolean) {
		super.setActivated(activated)
		binding.infoField.visibility = if (activated) VISIBLE else GONE
	}
}
