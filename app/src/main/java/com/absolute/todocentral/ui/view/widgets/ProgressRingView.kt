package com.absolute.todocentral.ui.view.widgets

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
 * The bento hero's completion ring: a track plus a sweep for the share of
 * today's work that is done. Colours come from the active dosha period, so it
 * re-themes with everything else.
 */
class ProgressRingView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()

    private var progress = 0f
    private var animator: ValueAnimator? = null

    init {
        val density = resources.displayMetrics.density
        trackPaint.strokeWidth = 7f * density
        sweepPaint.strokeWidth = 7f * density
        trackPaint.color = themeColor(R.attr.somaStrong)
        sweepPaint.color = themeColor(R.attr.somaPrimary)
    }

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    /** [value] is 0f..1f. Animates unless [animate] is false. */
    fun setProgress(value: Float, animate: Boolean = true) {
        val target = value.coerceIn(0f, 1f)
        animator?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 620
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
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
        super.onDraw(canvas)
        val inset = trackPaint.strokeWidth / 2f
        oval.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(oval, -90f, 360f * progress, false, sweepPaint)
        }
    }
}
