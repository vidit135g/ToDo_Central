package com.absolute.todocentral.ui.recycler

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.absolute.todocentral.R
import com.absolute.todocentral.data.TaskStats
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.utils.DateAndTimeFormatter
import com.absolute.todocentral.utils.gone
import com.absolute.todocentral.utils.visible

/**
 * The swipeable row of focus cards at the top of the home screen: what to look
 * at first, in the order it actually matters — late work, then starred, then
 * whatever is due today.
 *
 * A card's "progress" is its subtasks, which is the only real completion
 * figure a task carries. A task with no subtasks says so rather than showing
 * an invented percentage.
 */
class FocusCardAdapter : RecyclerView.Adapter<FocusCardAdapter.FocusViewHolder>() {

    private var cards = listOf<Task>()
    private var subtaskCounts: Map<Long, Pair<Int, Int>> = emptyMap()
    private lateinit var mContext: Context

    var onCardClick: ((Task) -> Unit)? = null

    /** Called whenever the card set changes, so the dot strip can follow. */
    var onCountChanged: ((Int) -> Unit)? = null

    fun submit(tasks: List<Task>, counts: Map<Long, Pair<Int, Int>>) {
        subtaskCounts = counts
        val dayStart = TaskStats.startOfToday()
        val dayEnd = dayStart + TaskStats.DAY_MS
        val open = tasks.filter { !it.isCompleted }

        val late = open.filter { it.date in 1 until dayStart }
        val starred = open.filter { it.priority == Task.PRIORITY_HIGH && it !in late }
        val today = open.filter { it.date in dayStart until dayEnd && it !in starred }

        // At most four: the row is a glance, not a second list.
        cards = (late + starred + today).distinct().take(4)
        notifyDataSetChanged()
        onCountChanged?.invoke(cards.size)
    }

    override fun getItemCount() = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusViewHolder {
        mContext = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.focus_card, parent, false)
        // Width is derived rather than fixed in the layout: the next card has
        // to peek by the same amount whatever the screen is.
        val res = parent.resources
        val gutter = res.getDimensionPixelSize(R.dimen.gutter)
        val peek = res.getDimensionPixelSize(R.dimen.focus_card_peek)
        view.layoutParams = view.layoutParams.apply {
            width = res.displayMetrics.widthPixels - (gutter * 2) - peek
        }
        return FocusViewHolder(view)
    }

    override fun onBindViewHolder(holder: FocusViewHolder, position: Int) {
        val task = cards[position]
        val dayStart = TaskStats.startOfToday()
        val late = task.date in 1 until dayStart

        // The lead card carries the accent ground; the rest are tinted, so the
        // row has a clear first thing to read.
        val accented = position == 0
        holder.card.setBackgroundResource(
                if (accented) R.drawable.bg_lav_card_big else R.drawable.bg_lav_card_big_alt)

        val ink = if (accented) themeColor(R.attr.somaOnFill) else themeColor(R.attr.somaTextP)
        val inkSoft = if (accented) themeColor(R.attr.somaTileCalm) else themeColor(R.attr.somaTextM)
        // Rose sits on the pill, which gives it a ground pale enough to read
        // on. Putting it on the card's body text too meant colouring it
        // against a mid-tone purple, where no shade of rose clears 3:1 — so
        // the "late" line just uses the card's own ink and the pill carries
        // the signal alone.
        val cardIsLight = isLight(
                if (accented) themeColor(R.attr.somaPrimary) else themeColor(R.attr.somaTileCalm))
        val pillInk = ContextCompat.getColor(mContext,
                if (cardIsLight) R.color.soma_overdue else R.color.soma_overdue_on_dark)

        holder.title.setTextColor(ink)
        holder.note.setTextColor(inkSoft)
        holder.stepsLabel.setTextColor(inkSoft)
        holder.steps.setTextColor(ink)
        holder.open.setColorFilter(themeColor(R.attr.somaPrimary), PorterDuff.Mode.SRC_IN)

        holder.tag.text = mContext.getString(when {
            late -> R.string.home_tag_late
            task.priority == Task.PRIORITY_HIGH -> R.string.home_tag_starred
            task.date != 0L && DateUtils.isToday(task.date) -> R.string.home_tag_today
            else -> R.string.home_tag_next
        })
        // The pill has to clear whichever ground it lands on: on the accent
        // card that means a pale fill with dark text, on the tinted card the
        // signal colour's own tint.
        holder.tag.setBackgroundResource(R.drawable.bg_lav_pill_accent)
        if (late) {
            // Late keeps rose type; on the accent card it needs a pale fill to
            // sit on, on a tinted one the rose tint is enough.
            holder.tag.backgroundTintList = ColorStateList.valueOf(
                    if (accented) themeColor(R.attr.somaElev)
                    else ContextCompat.getColor(mContext, R.color.soma_overdue_tint))
            holder.tag.setTextColor(pillInk)
        } else {
            holder.tag.backgroundTintList = null
            holder.tag.setTextColor(ContextCompat.getColor(mContext, R.color.soma_accent_ink))
        }

        holder.title.text = task.title

        if (task.note.isNotEmpty()) {
            holder.note.visible()
            holder.note.text = task.note.replace('\n', ' ').trim()
        } else {
            holder.note.gone()
        }

        if (task.date != 0L) {
            holder.whenRow.visible()
            val whenColor = if (late) ink else inkSoft
            holder.whenText.setTextColor(whenColor)
            holder.clock.setColorFilter(whenColor, PorterDuff.Mode.SRC_IN)
            holder.whenText.text = if (late) lateLabel(task.date, dayStart) else whenLabel(task.date)
        } else {
            holder.whenRow.gone()
        }

        val counts = subtaskCounts[task.id]
        val done = counts?.first ?: 0
        val total = counts?.second ?: 0
        if (total > 0) {
            holder.steps.text = mContext.getString(R.string.bento_subtask_progress, done, total)
            holder.track.setBackgroundResource(R.drawable.bg_lav_bar_track)
            // The fill is laid out as a fraction of the measured track, so it
            // has to wait until the track knows its own width.
            holder.track.post {
                holder.fill.layoutParams = holder.fill.layoutParams.apply {
                    width = (holder.track.width * done / total.toFloat()).toInt()
                }
                holder.fill.requestLayout()
            }
        } else {
            holder.steps.setText(R.string.home_no_steps)
            // The track stays put and goes dashed. Hiding it would shorten the
            // card and break the row's alignment.
            holder.track.setBackgroundResource(R.drawable.bg_lav_bar_empty)
            holder.fill.layoutParams = holder.fill.layoutParams.apply { width = 0 }
            holder.fill.requestLayout()
        }

        holder.itemView.setOnClickListener { onCardClick?.invoke(task) }
    }

    private fun whenLabel(date: Long): String = when {
        DateUtils.isToday(date) ->
            mContext.getString(R.string.reminder_today, DateAndTimeFormatter.getTime(date))
        DateUtils.isToday(date - DateUtils.DAY_IN_MILLIS) ->
            mContext.getString(R.string.reminder_tomorrow, DateAndTimeFormatter.getTime(date))
        else -> DateAndTimeFormatter.getFullDate(date)
    }

    private fun lateLabel(date: Long, dayStart: Long): String {
        val days = ((dayStart - TaskStats.startOfDay(date)) / TaskStats.DAY_MS).toInt()
        return if (days <= 1) mContext.getString(R.string.home_one_day_late)
        else mContext.getString(R.string.home_days_late, days)
    }

    /** Perceived lightness, so text can be picked for the ground it lands on. */
    private fun isLight(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) > 150
    }

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        mContext.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    class FocusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: LinearLayout = itemView.findViewById(R.id.llFocusCard)
        val tag: TextView = itemView.findViewById(R.id.tvFocusTag)
        val open: ImageView = itemView.findViewById(R.id.ivFocusOpen)
        val title: TextView = itemView.findViewById(R.id.tvFocusTitle)
        val note: TextView = itemView.findViewById(R.id.tvFocusNote)
        val whenRow: View = itemView.findViewById(R.id.llFocusWhen)
        val whenText: TextView = itemView.findViewById(R.id.tvFocusWhen)
        val clock: ImageView = itemView.findViewById(R.id.ivFocusClock)
        val stepsLabel: TextView = itemView.findViewById(R.id.tvFocusStepsLabel)
        val steps: TextView = itemView.findViewById(R.id.tvFocusSteps)
        val track: View = itemView.findViewById(R.id.flFocusTrack)
        val fill: View = itemView.findViewById(R.id.vFocusFill)
    }
}
