package com.absolute.todocentral.ui.view

import android.Manifest
import android.animation.TimeInterpolator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absolute.todocentral.BuildConfig
import com.absolute.todocentral.R
import com.absolute.todocentral.data.TaskStats
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.service.alarm.AlarmHelper
import com.absolute.todocentral.service.alarm.AlarmReceiver
import com.absolute.todocentral.service.widget.WidgetProvider
import com.absolute.todocentral.ui.dialogs.RateThisAppDialogFragment
import androidx.recyclerview.widget.PagerSnapHelper
import com.absolute.todocentral.ui.recycler.FocusCardAdapter
import com.absolute.todocentral.ui.view.custom.DayRingView
import com.absolute.todocentral.ui.recycler.RecyclerViewAdapter
import com.absolute.todocentral.ui.recycler.RecyclerViewScrollListener
import com.absolute.todocentral.ui.view.base.BaseActivity
import com.absolute.todocentral.ui.view.settings.activity.SettingsActivity
import com.absolute.todocentral.ui.view.settings.fragment.FragmentDateAndTime
import com.absolute.todocentral.ui.view.settings.fragment.FragmentNotifications
import com.absolute.todocentral.ui.view.task.AddTaskActivity
import com.absolute.todocentral.utils.SomaPeriod
import com.absolute.todocentral.utils.PreferenceHelper
import com.absolute.todocentral.utils.gone
import com.absolute.todocentral.utils.toast
import com.absolute.todocentral.utils.visible
import com.absolute.todocentral.vm.TaskListViewModel
import com.google.android.material.snackbar.Snackbar
import com.absolute.todocentral.utils.applySomaTheme
import kotterknife.bindView
import top.wefor.circularanim.CircularAnim
import java.util.*

class MainActivity : BaseActivity() {
    private val mRecyclerView: RecyclerView by bindView(R.id.rvTasksList)
    private val llNav: LinearLayout by bindView(R.id.llNav)
    private val navSearch: View by bindView(R.id.navSearch)
    private val navAdd: View by bindView(R.id.navAdd)
    private val navSettings: View by bindView(R.id.navSettings)
    private val clMain: CoordinatorLayout by bindView(R.id.clMain)

    private var mSnackbar: Snackbar? = null

    private lateinit var mAdapter: RecyclerViewAdapter
    private val mFocusAdapter = FocusCardAdapter()
    private var mFocusCount = 0
    private var mFocusPage = 0
    private var mRingSeeded = false
    private var mFocusSnap: PagerSnapHelper? = null
    private var mStats: TaskStats? = null
    private var mSubtaskCounts: Map<Long, Pair<Int, Int>> = emptyMap()
    private lateinit var mPreferenceHelper: PreferenceHelper
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mViewModel: TaskListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySomaTheme()
        setContentView(R.layout.activity_main)

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmHelper.getInstance().init(applicationContext)

        mViewModel = createViewModel()
        mViewModel.liveData.observe(this, Observer<List<Task>> { response -> updateViewState(response) })

        PreferenceHelper.getInstance().init(applicationContext)
        mPreferenceHelper = PreferenceHelper.getInstance()

        // The nav pill floats, so it takes the bottom inset as margin rather
        // than padding, keeping it clear of the gesture bar.
        val navGap = (llNav.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        applyEdgeToEdge(clMain, padTop = mRecyclerView, padBottom = mRecyclerView) { _, bottom ->
            (llNav.layoutParams as ViewGroup.MarginLayoutParams).let {
                if (it.bottomMargin != navGap + bottom) {
                    it.bottomMargin = navGap + bottom
                    llNav.requestLayout()
                }
            }
        }
        // Safe: rvTasksList is match_parent, so adapter updates never change
        // its size. Lint cannot distinguish it from the wrap_content carousel
        // that lives inside the header.
        @Suppress("InvalidSetHasFixedSize")
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mAdapter = RecyclerViewAdapter()
        mAdapter.headerBinder = { bindHomeHeader(it) }
        mRecyclerView.adapter = mAdapter
        initListListener()
        mFocusAdapter.onCardClick = { showTaskDetailsActivity(it) }
        mFocusAdapter.onCountChanged = { mFocusCount = it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isHasPermissions(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPerms(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        showChangelogActivity()
        showRecyclerViewAnimation()
        showRateThisAppDialog()
        createItemTouchHelper()
        initListeners()
        initCallbacks()
        initShortcuts()
    }

    /**
     * Every row interaction — opening a task, ticking it off, starring it —
     * comes back through this one listener.
     */
    private fun initListListener() {
        mAdapter.listListener = object : RecyclerViewAdapter.ListListener {
            override fun onTaskClick(task: Task) = showTaskDetailsActivity(task)

            override fun onToggleCompleted(task: Task) = toggleCompleted(task)

            override fun onToggleStar(task: Task) {
                task.priority =
                        if (task.priority == Task.PRIORITY_HIGH) Task.PRIORITY_NORMAL
                        else Task.PRIORITY_HIGH
                mViewModel.updateTask(task)
            }

            override fun onAddTaskClick() = openAddTask(llNav)
        }
    }

    /**
     * Fills the home header each time it is bound. It is a recycled row like
     * any other, so everything it shows is re-read here rather than wired once
     * at startup.
     */
    private fun bindHomeHeader(view: View) {
        val stats = mStats

        view.findViewById<TextView>(R.id.tvGreeting).setText(greetingRes())
        // The greeting follows the clock, this follows the theme, and the two
        // disagree whenever a period is pinned — so say which mode it is in.
        val periodName = getString(SomaPeriod.labelResId(SomaPeriod.resolveId()))
        val pinned = SomaPeriod.getPreference() != SomaPeriod.AUTO
        view.findViewById<TextView>(R.id.tvPeriodName).text =
                getString(if (pinned) R.string.home_period_pinned else R.string.home_period_auto, periodName)
        view.findViewById<View>(R.id.flPeriod).setOnClickListener { cycleSomaPeriod() }

        val dayStart = TaskStats.startOfToday()
        val dayEnd = dayStart + TaskStats.DAY_MS
        val open = mTaskList.filter { !it.isCompleted }
        val late = open.count { it.date in 1 until dayStart }
        val dueToday = open.count { it.date in dayStart until dayEnd }
        val onPlate = late + dueToday

        view.findViewById<TextView>(R.id.tvHeadline).text = when (onPlate) {
            0 -> getString(R.string.home_headline_clear)
            1 -> getString(R.string.home_headline_one)
            else -> getString(R.string.home_headline_many, onPlate)
        }

        view.findViewById<View>(R.id.vBellDot).visibility =
                if (late > 0) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.flBell).setOnClickListener {
            mAdapter.filter = RecyclerViewAdapter.FILTER_TODO
        }
        view.findViewById<View>(R.id.flAdd).setOnClickListener { openAddTask(it) }
        view.findViewById<View>(R.id.tvSeeAll).setOnClickListener {
            mAdapter.filter = RecyclerViewAdapter.FILTER_TODO
        }

        val focus = view.findViewById<RecyclerView>(R.id.rvFocus)
        val dots = view.findViewById<LinearLayout>(R.id.llDots)
        if (focus.adapter == null) {
            focus.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            focus.adapter = mFocusAdapter
            val snap = PagerSnapHelper()
            snap.attachToRecyclerView(focus)
            mFocusSnap = snap
            focus.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    applyCardDepth(rv)
                    // The snapped card, not the first visible one — the card
                    // scrolling off the left edge still counts as visible, so
                    // the dots never moved off page one.
                    val lm = rv.layoutManager ?: return
                    val snapped = snap.findSnapView(lm) ?: return
                    val page = lm.getPosition(snapped)
                    if (page != RecyclerView.NO_POSITION && page != mFocusPage) {
                        mFocusPage = page
                        bindDots(dots, mFocusCount, page)
                    }
                }
            })
        }
        mFocusAdapter.submit(mTaskList, mSubtaskCounts)
        focus.post { applyCardDepth(focus) }
        bindDots(dots, mFocusCount, mFocusPage)

        // The ring is the surprise the header carries: today's work, ticked.
        val ring = view.findViewById<DayRingView>(R.id.ringDay)
        val todayAll = mTaskList.count { it.date in dayStart until dayEnd }
        val todayDone = mTaskList.count { it.isCompleted && it.date in dayStart until dayEnd }
        ring.setProgress(if (todayAll == 0) 0f else todayDone / todayAll.toFloat(), mRingSeeded)
        mRingSeeded = true

        bindChips(view, open.size, dueToday, mTaskList.count { it.isCompleted })
        if (stats == null) refreshStats()
    }

    /**
     * Cards shrink and fade with distance from the left edge of the row, so
     * the one you are reading is plainly the one in front.
     */
    private fun applyCardDepth(rv: RecyclerView) {
        // Distance from the resting slot, measured both ways, so the card that
        // has snapped into place is the full-size one whichever side you came
        // from.
        val rest = rv.paddingStart.toFloat()
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            if (child.width == 0) continue
            val offset = kotlin.math.abs(child.left - rest) / child.width.toFloat()
            val f = (1f - offset).coerceIn(0f, 1f)
            child.scaleY = 0.94f + 0.06f * f
            child.alpha = 0.70f + 0.30f * f
        }
    }

    /** One dot per focus card; the current page is a wide bar. */
    private fun bindDots(strip: LinearLayout, count: Int, active: Int) {
        strip.removeAllViews()
        if (count <= 1) return
        val d = resources.displayMetrics.density
        for (i in 0 until count) {
            val on = i == active
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(
                    ((if (on) 16 else 5) * d).toInt(), (5 * d).toInt()).apply {
                if (i > 0) marginStart = (5 * d).toInt()
            }
            dot.setBackgroundResource(
                    if (on) R.drawable.bg_lav_dot_on else R.drawable.bg_lav_dot_off)
            strip.addView(dot)
        }
    }

    private fun bindChips(view: View, todo: Int, today: Int, done: Int) {
        val chips = listOf(
                Triple(view.findViewById<TextView>(R.id.chipTodo),
                        getString(R.string.home_chip_todo, todo), RecyclerViewAdapter.FILTER_TODO),
                Triple(view.findViewById<TextView>(R.id.chipToday),
                        getString(R.string.home_chip_today, today), RecyclerViewAdapter.FILTER_TODAY),
                Triple(view.findViewById<TextView>(R.id.chipDone),
                        getString(R.string.home_chip_done, done), RecyclerViewAdapter.FILTER_DONE))

        for ((chip, label, value) in chips) {
            chip.text = label
            val on = mAdapter.filter == value
            chip.setBackgroundResource(
                    if (on) R.drawable.bg_lav_chip_on else R.drawable.bg_lav_chip_off)
            chip.setTextColor(themeColor(if (on) R.attr.somaOnFill else R.attr.somaTextS))
            chip.setOnClickListener { mAdapter.filter = value }
        }
    }

    private fun greetingRes(): Int =
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> R.string.home_greet_morning
                in 12..16 -> R.string.home_greet_afternoon
                in 17..21 -> R.string.home_greet_evening
                else -> R.string.home_greet_night
            }

    private fun themeColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    private fun openAddTask(view: View) {
        if (mSnackbar?.isShown == true) {
            mSnackbar?.dismiss()
            mSnackbar = null
        }
        val intent = Intent(this, AddTaskActivity::class.java)
                .putExtra("position", mAdapter.taskCount)
        if (mPreferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)) {
            CircularAnim.fullActivity(this, view)
                    .colorOrImageRes(R.color.blue)
                    .duration(300)
                    .go { startActivity(intent) }
        } else {
            startActivity(intent)
        }
    }

    /** Ticks a task off, or un-ticks it; recurring tasks roll to their next date. */
    private fun toggleCompleted(task: Task) {
        if (task.isCompleted) {
            task.completedAt = 0
        } else {
            task.completedAt = System.currentTimeMillis()
            if (task.isRecurring && task.date != 0L) {
                val next = Calendar.getInstance().apply {
                    timeInMillis = task.date
                    when (task.repeat) {
                        Task.REPEAT_DAILY -> add(Calendar.DAY_OF_YEAR, 1)
                        Task.REPEAT_WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                        Task.REPEAT_MONTHLY -> add(Calendar.MONTH, 1)
                    }
                }
                // A repeating task reopens on its next date rather than being
                // consumed, so the series keeps running.
                task.date = next.timeInMillis
                task.completedAt = 0
            }
        }
        mViewModel.updateTask(task)
        refreshStats()
    }

    private fun cycleSomaPeriod() {
        val current = SomaPeriod.getPreference()
        val next = when {
            current == SomaPeriod.AUTO -> SomaPeriod.ORDER[0]
            SomaPeriod.ORDER.indexOf(current) == SomaPeriod.ORDER.size - 1 -> SomaPeriod.AUTO
            else -> SomaPeriod.ORDER[SomaPeriod.ORDER.indexOf(current) + 1]
        }
        SomaPeriod.setPreference(next)
        recreate()
    }

    private fun refreshStats() {
        Thread {
            val stats = mViewModel.statsNow()
            val counts = mViewModel.subtaskCounts()
            runOnUiThread {
                mStats = stats
                mSubtaskCounts = counts
                mAdapter.updateStats(stats, counts)
            }
        }.start()
    }

    private fun updateViewState(tasks: List<Task>) {
        if (tasks.isEmpty()) showEmptyView() else showTaskList(tasks)
        refreshStats()
    }

    private fun showTaskList(tasks: List<Task>) {
        var isNeedToRecount = false
        if (mTaskList.size > tasks.size) isNeedToRecount = true
        mTaskList = tasks as ArrayList<Task>
        if (isNeedToRecount) recountTaskPositions()
        mAdapter.updateData(mTaskList)
        mPreferenceHelper.putInt(PreferenceHelper.NEW_TASK_POSITION, mAdapter.taskCount)
        restoreAlarmsAfterMigration()
        updateGeneralNotification()
        updateWidget()
    }

    private fun recountTaskPositions() {
        for ((newPosition, task) in mTaskList.withIndex()) {
            task.position = newPosition
        }
        mViewModel.updateTaskOrder(mTaskList)
    }

    /**
     * The empty state is a row inside the list, so the greeting, headline and
     * filter chips stay on screen. The old centred overlay drew over them.
     */
    private fun showEmptyView() {
        mTaskList = arrayListOf()
        mAdapter.updateData(mTaskList)
        updateGeneralNotification()
        updateWidget()
    }

    private fun showRecyclerViewAnimation() {
        if (mPreferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)) {
            val resId = R.anim.layout_animation
            val animation = AnimationUtils.loadLayoutAnimation(this, resId)
            mRecyclerView.layoutAnimation = animation
        }
    }

    private fun showRateThisAppDialog() {
        var counter = mPreferenceHelper.getInt(PreferenceHelper.LAUNCHES_COUNTER)
        if (mPreferenceHelper.getBoolean(PreferenceHelper.IS_NEED_TO_SHOW_RATE_DIALOG_LATER) && counter == 4) {
            RateThisAppDialogFragment().show(supportFragmentManager, null)
        } else {
            mPreferenceHelper.putInt(PreferenceHelper.LAUNCHES_COUNTER, ++counter)
        }
    }

    private fun createItemTouchHelper() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            // Rows are grouped by due date now, so there is no manual order to
            // drag. Only task rows swipe; section headings and the add row are
            // inert.
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder.itemViewType != RecyclerViewAdapter.TYPE_TASK) {
                    return ItemTouchHelper.Callback.makeMovementFlags(0, 0)
                }
                return ItemTouchHelper.Callback.makeMovementFlags(
                        0, ItemTouchHelper.START or ItemTouchHelper.END)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTask(viewHolder.adapterPosition)
            }
        })
        helper.attachToRecyclerView(mRecyclerView)
    }

    private fun deleteTask(position: Int) {
        val deletedTask = mAdapter.taskAt(position) ?: return
        val isDeletedTaskHasLastPosition = deletedTask.position == mAdapter.taskCount - 1
        val alarmHelper = AlarmHelper.getInstance()
        alarmHelper.removeAlarm(deletedTask.timeStamp)

        mAdapter.removeTask(position)
        mViewModel.deleteTask(deletedTask)
        var isUndoClicked = false

        mSnackbar = Snackbar.make(mRecyclerView, R.string.snackbar_remove_task, Snackbar.LENGTH_LONG)
        mSnackbar?.setAction(R.string.snackbar_undo) {
            mViewModel.saveTask(deletedTask)
            if (deletedTask.date != 0L && deletedTask.date > Calendar.getInstance().timeInMillis) {
                alarmHelper.setAlarm(deletedTask)
            }
            isUndoClicked = true

            Handler(Looper.getMainLooper()).postDelayed({
                val firstCompletelyVisibleItem = (mRecyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            }, 100)
        }

        mSnackbar?.view?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                val firstCompletelyVisibleItem = (mRecyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                val lastCompletelyVisibleItem = (mRecyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

            }

            override fun onViewDetachedFromWindow(view: View) {
                if (!isUndoClicked) {
                    alarmHelper.removeNotification(deletedTask.timeStamp, this@MainActivity)
                    if (!isDeletedTaskHasLastPosition) recountTaskPositions()
                }
            }
        })
        mSnackbar?.anchorView = llNav
        mSnackbar?.show()
    }

    private fun moveTask(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            // Move down
            for (i in fromPosition until toPosition) {
                Collections.swap(mTaskList, i, i + 1)
                mTaskList[i].position = i
                mTaskList[i + 1].position = i + 1
            }
        } else {
            // Move up
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(mTaskList, i, i - 1)
                mTaskList[i].position = i
                mTaskList[i - 1].position = i - 1
            }
        }
        mViewModel.updateTaskOrder(mTaskList)
        mAdapter.updateData(mTaskList)
        updateGeneralNotification()
        updateWidget()
    }

    private fun showChangelogActivity() {
        if (mPreferenceHelper.getBoolean(PreferenceHelper.IS_FIRST_LAUNCH) && mPreferenceHelper.getInt(PreferenceHelper.VERSION_CODE) == 0) {
        } else if (mPreferenceHelper.getInt(PreferenceHelper.VERSION_CODE) != BuildConfig.VERSION_CODE) {
            startActivity(Intent(this, ChangelogActivity::class.java))
        }
    }

    private fun restoreAlarmsAfterMigration() {
        if (mPreferenceHelper.getBoolean(PreferenceHelper.IS_AFTER_DATABASE_MIGRATION)) {
            val alarmHelper = AlarmHelper.getInstance()

            for (task in mTaskList) {
                if (task.date != 0L && task.date > Calendar.getInstance().timeInMillis) {
                    alarmHelper.setAlarm(task)
                }
            }
            mPreferenceHelper.putBoolean(PreferenceHelper.IS_AFTER_DATABASE_MIGRATION, false)
        }
    }

    private fun initListeners() {
        navAdd.setOnClickListener { openAddTask(it) }

        navAdd.setOnLongClickListener {
            if (isHasPermissions(Manifest.permission.RECORD_AUDIO)) {
                showVoiceInput()
            } else {
                requestPermissionWithRationale(clMain, getString(R.string.permission_microphone_snackbar_with_rationale),
                        Manifest.permission.RECORD_AUDIO, null, navAdd)
            }
            true
        }

        navSearch.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        navSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        // The pill drops out of the way while the list is moving, so a long
        // list is never read through it.
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 6 && llNav.translationY == 0f) {
                    llNav.animate().translationY(160f).setDuration(180).start()
                } else if (dy < -6 && llNav.translationY != 0f) {
                    llNav.animate().translationY(0f).setDuration(180).start()
                }
            }
        })
    }

    private fun animateToolbar(translationValue: Float, interpolator: TimeInterpolator) {
        toolbar?.animate()?.translationY(translationValue)?.setInterpolator(interpolator)
    }

    private fun showVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_to_text_hint))
        }
        try {
            startActivityForResult(intent, SPEECH_INPUT_CODE)
        } catch (exception: ActivityNotFoundException) {
            toast(getString(R.string.speech_to_text_error))
        }
    }

    private fun createViewModel() = ViewModelProvider(this)[TaskListViewModel::class.java]

    private fun updateWidget() {
        val intent = Intent(this, WidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    private fun updateGeneralNotification() {
        if (mPreferenceHelper.getBoolean(PreferenceHelper.GENERAL_NOTIFICATION_IS_ON)) {
            if (mAdapter.taskCount != 0) showGeneralNotification() else removeGeneralNotification()
        } else removeGeneralNotification()
    }

    private fun showGeneralNotification() {
        val stringBuilder = StringBuilder()
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        for (task in mTaskList) {
            stringBuilder.append("• ").append(task.title)

            if (task.position < mAdapter.taskCount - 1) {
                stringBuilder.append("\n")
            }
        }

        var notificationTitle = ""
        when (mAdapter.taskCount % 10) {
            1 -> notificationTitle = "${getString(R.string.general_notification_1)} ${mAdapter.taskCount} ${getString(R.string.general_notification_2)}"

            2, 3, 4 -> notificationTitle = "${getString(R.string.general_notification_1)} ${mAdapter.taskCount} ${getString(R.string.general_notification_3)}"

            0, 5, 6, 7, 8, 9 -> notificationTitle = "${getString(R.string.general_notification_1)} ${mAdapter.taskCount} ${getString(R.string.general_notification_4)}"
        }

        // Set NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(AlarmReceiver.GENERAL_NOTIFICATION_CHANNEL_ID, getString(R.string.general_notification_channel),
                    NotificationManager.IMPORTANCE_LOW)
            channel.enableLights(false)
            channel.enableVibration(false)
            mNotificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, AlarmReceiver.GENERAL_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(stringBuilder.toString())
                .setNumber(mAdapter.taskCount)
                .setStyle(NotificationCompat.BigTextStyle().bigText(stringBuilder.toString()))
                .setColor(getColor(this, R.color.blue))
                .setSmallIcon(R.drawable.ic_check_circle_white_24dp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
        mNotificationManager.notify(1, notification.build())
    }

    private fun removeGeneralNotification() = mNotificationManager.cancel(1)

    private fun initCallbacks() {
        val callbackGeneralNotification = object : FragmentNotifications.GeneralNotificationClickListener {
            override fun onGeneralNotificationStateChanged() = updateGeneralNotification()
        }
        FragmentNotifications.callback = callbackGeneralNotification

        val callbackDateAndTimeFormat = object : FragmentDateAndTime.DateAndTimeFormatCallback {
            override fun onDateAndTimeFormatChanged() {
                mAdapter.reloadTasks()
                updateWidget()
            }
        }
        FragmentDateAndTime.callback = callbackDateAndTimeFormat
    }

    private fun initShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val newTaskShortcut = ShortcutInfo.Builder(this, "newTask")
                    .setShortLabel(getString(R.string.shortcut_add_new_task))
                    .setLongLabel(getString(R.string.shortcut_add_new_task))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_add))
                    .setIntents(arrayOf(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW),
                            Intent(this, AddTaskActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra("isShortcut", true)))
                    .build()

            val searchShortcut = ShortcutInfo.Builder(this, "searchTask")
                    .setShortLabel(getString(R.string.shortcut_search))
                    .setLongLabel(getString(R.string.shortcut_search))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_search))
                    .setIntents(arrayOf(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW),
                            Intent(this, SearchActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra("isShortcut", true)))
                    .build()

            getSystemService(ShortcutManager::class.java).dynamicShortcuts = Arrays.asList(searchShortcut, newTaskShortcut)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isAllowed = true

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                for (res in grantResults) {
                    // If user granted all permissions.
                    isAllowed = isAllowed && res == PackageManager.PERMISSION_GRANTED
                }
            }

            else -> {
                // If user not granted permissions.
                isAllowed = false
            }
        }

        if (isAllowed) {
            showVoiceInput()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                toast(getString(R.string.permission_microphone_denied_toast))
            } else {
                showNoPermissionSnackbar(clMain, getString(R.string.permission_microphone_snackbar_no_permission),
                        getString(R.string.permission_microphone_toast), navAdd)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_search -> startActivity(Intent(this, SearchActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == APP_INTRO_CODE) {
            if (resultCode == RESULT_OK) {
                mPreferenceHelper.putBoolean(PreferenceHelper.IS_FIRST_LAUNCH, false)
                mPreferenceHelper.putInt(PreferenceHelper.VERSION_CODE, BuildConfig.VERSION_CODE)
            } else {
                finish()
            }
        }

        if (requestCode == SPEECH_INPUT_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                val recognizedText = result[0]
                val capitalizedTitle = recognizedText.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                val task = Task().apply {
                    title = capitalizedTitle
                    this.position = mAdapter.taskCount
                }
                mViewModel.saveTask(task)
            }
        }
    }

    companion object {
        var mTaskList = arrayListOf<Task>()
        private const val SPEECH_INPUT_CODE = 111
        private const val APP_INTRO_CODE = 222
    }
}
