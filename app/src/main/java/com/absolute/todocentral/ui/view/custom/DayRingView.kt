package com.absolute.todocentral.ui.view.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.absolute.todocentral.R

/**
 * The ring around the period button: how much of today is already ticked off.
 *
 * It shares its spot with the theme switcher on purpose — the Soma periods are
 * a reading of where you are in the day, and so is this, so the two belong on
 * the same control rather than competing for room in the header.
 */
class DayRingView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val oval = RectF()
    private var stroke = dp(2.5f)
    private var shown = 0f
    private var target = 0f
    private var animator: ValueAnimator? = null

    init {
        trackPaint.color = themeColor(R.attr.somaStrong)
        progressPaint.color = themeColor(R.attr.somaPrimary)
        fillPaint.color = themeColor(R.attr.somaTileCalm)
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
    }

    /**
     * [fraction] is clamped to 0..1. The first value after the view appears is
     * set outright; later ones sweep, so ticking a task off reads as movement
     * rather than a jump.
     */
    fun setProgress(fraction: Float, animate: Boolean = true) {
        target = fraction.coerceIn(0f, 1f)
        animator?.cancel()
        if (!animate || shown == target) {
            shown = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(shown, target).apply {
            duration = 520
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val pad = stroke / 2f
        oval.set(pad, pad, width - pad, height - pad)

        canvas.drawCircle(width / 2f, height / 2f, (width / 2f) - stroke, fillPaint)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        // -90 starts the sweep at the top, which is where a clock face starts
        // and so where the eye looks first.
        if (shown > 0f) canvas.drawArc(oval, -90f, 360f * shown, false, progressPaint)
    }

    private fun dp(v: Float) =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }
}
