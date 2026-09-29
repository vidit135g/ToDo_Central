package com.absolute.todocentral.ui.recycler

import android.content.Context
import android.graphics.Paint
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
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
 * Flat, checkbox-led task list grouped by when things are due, with completed
 * work collected at the bottom.
 *
 * The list the RecyclerView sees is a flattened mix of section headings, task
 * rows and one "add a task" row, so adapter positions do not line up with task
 * indices. Everything that receives a position from the RecyclerView resolves
 * it through [taskAt], which returns null for non-task rows.
 */
class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        object Home : Row()
        data class Section(val titleRes: Int, val count: Int) : Row()
        data class TaskRow(val task: Task, val lastInSection: Boolean) : Row()
        object Empty : Row()
    }

    private var rows = listOf<Row>()
    private var mTaskList = arrayListOf<Task>()
    private var subtaskCounts: Map<Long, Pair<Int, Int>> = emptyMap()
    private lateinit var mContext: Context

    var listListener: ListListener? = null

    /**
     * Search results are already a filtered answer to a query, so they show as
     * a plain list — no date groups, no home header.
     */
    var grouped = true

    /** Which chip in the home header is active. */
    var filter = FILTER_TODO
        set(value) {
            field = value
            rebuildRows()
        }

    /** Binds the home header's own views; supplied by the activity. */
    var headerBinder: ((View) -> Unit)? = null

    fun updateData(tasks: List<Task>) {
        mTaskList = ArrayList(tasks)
        rebuildRows()
    }

    fun updateStats(@Suppress("UNUSED_PARAMETER") stats: TaskStats,
                    counts: Map<Long, Pair<Int, Int>>) {
        subtaskCounts = counts
        notifyDataSetChanged()
    }

    /**
     * Groups into Overdue / Today / Tomorrow / Upcoming / No date, then
     * Completed. Rebuilt wholesale: the list is small and a full rebind avoids
     * the position drift that partial updates cause with mixed row types.
     */
    private fun rebuildRows() {
        val dayStart = TaskStats.startOfToday()
        val dayEnd = dayStart + TaskStats.DAY_MS
        val tomorrowEnd = dayEnd + TaskStats.DAY_MS

        val open = mTaskList.filter { !it.isCompleted }
        val done = mTaskList.filter { it.isCompleted }

        if (filter == FILTER_DONE) {
            val built = mutableListOf<Row>(Row.Home)
            if (done.isNotEmpty()) {
                built.add(Row.Section(R.string.section_completed, done.size))
                done.forEachIndexed { i, t -> built.add(Row.TaskRow(t, i == done.lastIndex)) }
            }
            if (built.size == 1) built.add(Row.Empty)
            rows = built
            notifyDataSetChanged()
            return
        }
        if (filter == FILTER_TODAY) {
            val onlyToday = open.filter { it.date in dayStart until dayEnd }
            val built = mutableListOf<Row>(Row.Home)
            if (onlyToday.isNotEmpty()) {
                built.add(Row.Section(R.string.section_today, onlyToday.size))
                onlyToday.forEachIndexed { i, t -> built.add(Row.TaskRow(t, i == onlyToday.lastIndex)) }
            }
            if (built.size == 1) built.add(Row.Empty)
            rows = built
            notifyDataSetChanged()
            return
        }

        val overdue = open.filter { it.date in 1 until dayStart }
        val today = open.filter { it.date in dayStart until dayEnd }
        val tomorrow = open.filter { it.date in dayEnd until tomorrowEnd }
        val later = open.filter { it.date >= tomorrowEnd }
        val undated = open.filter { it.date == 0L }

        if (!grouped) {
            rows = mTaskList.mapIndexed { i, t -> Row.TaskRow(t, i == mTaskList.lastIndex) }
            notifyDataSetChanged()
            return
        }

        val built = mutableListOf<Row>(Row.Home)
        fun addSection(titleRes: Int, items: List<Task>) {
            if (items.isEmpty()) return
            built.add(Row.Section(titleRes, items.size))
            items.forEachIndexed { i, t ->
                built.add(Row.TaskRow(t, i == items.lastIndex))
            }
        }
        addSection(R.string.section_overdue, overdue)
        addSection(R.string.section_today, today)
        addSection(R.string.section_tomorrow, tomorrow)
        addSection(R.string.section_upcoming, later)
        addSection(R.string.section_no_date, undated)
        if (built.size == 1) built.add(Row.Empty)

        rows = built
        notifyDataSetChanged()
    }

    /** The task at an adapter position, or null for a heading / add row. */
    fun taskAt(position: Int): Task? =
            (rows.getOrNull(position) as? Row.TaskRow)?.task

    fun getTaskAtPosition(position: Int): Task =
            taskAt(position) ?: throw IllegalArgumentException("position $position is not a task")

    fun removeTask(position: Int) {
        val task = taskAt(position) ?: return
        mTaskList.remove(task)
        rebuildRows()
    }

    fun updateTaskOrder(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    fun reloadTasks() = rebuildRows()

    fun tasks(): List<Task> = mTaskList

    val taskCount: Int get() = mTaskList.size

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) = when (rows[position]) {
        is Row.Home -> TYPE_HOME
        is Row.Section -> TYPE_SECTION
        is Row.TaskRow -> TYPE_TASK
        is Row.Empty -> TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mContext = parent.context
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HOME -> HomeViewHolder(inflater.inflate(R.layout.home_header, parent, false))
            TYPE_SECTION -> SectionViewHolder(inflater.inflate(R.layout.task_section_header, parent, false))
            TYPE_EMPTY -> EmptyViewHolder(inflater.inflate(R.layout.task_empty, parent, false))
            else -> TaskViewHolder(inflater.inflate(R.layout.task_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Home -> headerBinder?.invoke(holder.itemView)
            is Row.Section -> (holder as SectionViewHolder).run {
                title.setText(row.titleRes)
                count.text = row.count.toString()
            }
            is Row.TaskRow -> bindTask(holder as TaskViewHolder, row.task, row.lastInSection)
            is Row.Empty -> (holder as EmptyViewHolder).run {
                val (t, b) = when (filter) {
                    FILTER_TODAY -> R.string.empty_today_title to R.string.empty_today_body
                    FILTER_DONE -> R.string.empty_done_title to R.string.empty_done_body
                    else -> R.string.empty_todo_title to R.string.empty_todo_body
                }
                title.setText(t); body.setText(b)
            }
        }
    }

    private fun bindTask(holder: TaskViewHolder, task: Task, @Suppress("UNUSED_PARAMETER") lastInSection: Boolean) {
        holder.card.setOnClickListener { listListener?.onTaskClick(task) }

        holder.check.scaleX = 1f
        holder.check.scaleY = 1f

        holder.title.text = task.title
        holder.title.paintFlags =
                if (task.isCompleted) holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        holder.title.setTextColor(
                if (task.isCompleted) themeColor(R.attr.somaTextM) else themeColor(R.attr.somaTextP))

        // Both tick drawables carry their own theme-attr colours, so no filter
        // here — one would flatten the knocked-out tick into the disc.
        holder.check.setImageResource(
                if (task.isCompleted) R.drawable.ic_soma_check_on else R.drawable.ic_soma_check_off)
        holder.checkTarget.setOnClickListener {
            // A tick is the one thing people do over and over here, so it gets
            // a beat of its own: the circle dips, springs back, and the row's
            // new state lands as the data comes round again.
            holder.check.animate()
                    .scaleX(0.7f).scaleY(0.7f).setDuration(90)
                    .withEndAction {
                        holder.check.animate()
                                .scaleX(1f).scaleY(1f)
                                .setInterpolator(OvershootInterpolator(3.2f))
                                .setDuration(260)
                                .start()
                        listListener?.onToggleCompleted(task)
                    }
                    .start()
        }

        // High priority reads as starred, which is the familiar shorthand.
        val starred = task.priority == Task.PRIORITY_HIGH
        holder.star.setImageResource(
                if (starred) R.drawable.ic_soma_star_on else R.drawable.ic_soma_star_off)
        holder.star.setColorFilter(
                if (starred) ContextCompat.getColor(mContext, R.color.soma_star)
                else themeColor(R.attr.somaTextM))
        holder.star.setOnClickListener { listListener?.onToggleStar(task) }

        if (task.note.isNotEmpty()) {
            holder.note.visible()
            holder.note.text = task.note.replace('\n', ' ').trim()
        } else {
            holder.note.gone()
        }

        if (task.date != 0L) {
            holder.date.visible()
            holder.date.text = whenLabel(task)
            // Late means a past *day*, not merely a time that has gone by:
            // everything under Today would otherwise turn red by mid-afternoon.
            val overdue = !task.isCompleted && task.date < TaskStats.startOfToday()
            holder.date.setTextColor(
                    if (overdue) ContextCompat.getColor(mContext, R.color.soma_overdue)
                    else themeColor(R.attr.somaTextS))
        } else {
            holder.date.gone()
        }

        if (task.isRecurring) holder.repeat.visible() else holder.repeat.gone()

        val counts = subtaskCounts[task.id]
        if (counts != null && counts.second > 0) {
            holder.subtaskProgress.visible()
            holder.subtaskProgress.text =
                    mContext.getString(R.string.bento_subtask_progress, counts.first, counts.second)
        } else {
            holder.subtaskProgress.gone()
        }

        holder.meta.visibility =
                if (holder.date.visibility == View.GONE && holder.repeat.visibility == View.GONE &&
                        holder.subtaskProgress.visibility == View.GONE) View.GONE else View.VISIBLE

        holder.open.setOnClickListener { listListener?.onTaskClick(task) }
    }

    private fun whenLabel(task: Task): String = when {
        task.date == 0L -> mContext.getString(R.string.bento_no_date)
        DateUtils.isToday(task.date) ->
            mContext.getString(R.string.reminder_today, DateAndTimeFormatter.getTime(task.date))
        DateUtils.isToday(task.date - DateUtils.DAY_IN_MILLIS) ->
            mContext.getString(R.string.reminder_tomorrow, DateAndTimeFormatter.getTime(task.date))
        DateUtils.isToday(task.date + DateUtils.DAY_IN_MILLIS) ->
            mContext.getString(R.string.reminder_yesterday, DateAndTimeFormatter.getTime(task.date))
        else -> DateAndTimeFormatter.getFullDate(task.date)
    }

    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        mContext.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    interface ListListener {
        fun onTaskClick(task: Task)
        fun onToggleCompleted(task: Task)
        fun onToggleStar(task: Task)
        fun onAddTaskClick()
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val note: TextView = itemView.findViewById(R.id.tvTaskNote)
        val date: TextView = itemView.findViewById(R.id.tvTaskDate)
        val check: ImageView = itemView.findViewById(R.id.ivCheck)
        val checkTarget: View = itemView.findViewById(R.id.flCheck)
        val star: ImageView = itemView.findViewById(R.id.ivStar)
        val repeat: ImageView = itemView.findViewById(R.id.ivRepeat)
        val subtaskProgress: TextView = itemView.findViewById(R.id.tvSubtaskProgress)
        val meta: View = itemView.findViewById(R.id.llTaskMeta)
        val card: View = itemView.findViewById(R.id.llTaskCard)
        val open: View = itemView.findViewById(R.id.flOpen)
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvSectionTitle)
        val count: TextView = itemView.findViewById(R.id.tvSectionCount)
    }

    class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvEmptyTitle)
        val body: TextView = itemView.findViewById(R.id.tvEmptyBody)
    }

    companion object {
        const val TYPE_HOME = 0
        const val TYPE_SECTION = 1
        const val TYPE_TASK = 2
        const val TYPE_EMPTY = 3

        const val FILTER_TODO = 0
        const val FILTER_TODAY = 1
        const val FILTER_DONE = 2
    }
}
