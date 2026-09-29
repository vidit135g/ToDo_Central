package com.absolute.todocentral.ui.view.base

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.absolute.todocentral.R
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.ui.view.task.EditTaskActivity
import com.absolute.todocentral.utils.toastLong
import com.google.android.material.snackbar.Snackbar
import kotterknife.bindView
import android.widget.TextView

abstract class BaseActivity : AppCompatActivity() {
    val toolbar: Toolbar? by bindView(R.id.toolbar)
    val tvToolbarTitle: TextView by bindView(R.id.tvToolbarTitle)

    override fun onResume() {
        super.onResume()
        initStatusBar()
    }


    /**
     * Draws the app edge to edge and hands the system-bar insets to the
     * caller as padding.
     *
     * targetSdk 35 forces edge-to-edge on Android 15+, so this is opted into
     * explicitly instead: the same code path then runs on every supported
     * release, which means the behaviour shipping to Android 15 is the one
     * that can actually be tested here.
     */
    /**
     * Shifts the whole screen clear of the system bars — what the window used
     * to do before targetSdk 35 forced edge-to-edge. Screens that should
     * genuinely scroll under the bars use [applyEdgeToEdge] instead.
     */
    fun insetContentRoot() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(android.R.id.content)
        val top = root.paddingTop
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, top + bars.top, v.paddingRight, bottom + bars.bottom)
            insets
        }
    }

    fun applyEdgeToEdge(root: View, padTop: View? = null, padBottom: View? = null,
                        also: ((Int, Int) -> Unit)? = null) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val topStart = padTop?.paddingTop ?: 0
        val bottomStart = padBottom?.paddingBottom ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            padTop?.setPadding(padTop.paddingLeft, topStart + bars.top,
                    padTop.paddingRight, padTop.paddingBottom)
            padBottom?.setPadding(padBottom.paddingLeft, padBottom.paddingTop,
                    padBottom.paddingRight, bottomStart + bars.bottom)
            also?.invoke(bars.top, bars.bottom)
            insets
        }
    }

    fun initToolbar(titleText: String = "", drawable: Int? = R.drawable.round_arrow_back_black_24, view: Toolbar? = toolbar) {
        title = ""
        tvToolbarTitle.text = titleText

        // Under edge-to-edge these screens would sit beneath the status bar.
        // The inset goes on the content root, not the toolbar: a Toolbar's
        // height is fixed at actionBarSize, so padding it pushes the title
        // out of its own bounds instead of moving the screen down.
        if (view != null) insetContentRoot()

        if (toolbar != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // Pre-M cannot use the theme's windowLightStatusBar, so paint
                // the bar with the active period's own ground colour.
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = resolveThemeColor(R.attr.somaBg)
            }
            setSupportActionBar(view)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            if (drawable != null) {
                supportActionBar?.setHomeAsUpIndicator(drawable)
            }
        }
    }

    /**
     * The Soma period themes set statusBarColor and windowLightStatusBar
     * themselves, so on M+ there is nothing left to do here. Previously this
     * branched on the DressCode style id, which no longer exists.
     */
    private fun initStatusBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = resolveThemeColor(R.attr.somaBg)
        }
    }

    protected fun resolveThemeColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    private fun openApplicationSettings() =
            startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")), PERMISSION_REQUEST_CODE)

    fun requestPerms(permission: String, fragment: Fragment? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (fragment != null) {
                fragment.requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
            } else requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    fun showTaskDetailsActivity(task: Task) {
        val intent = Intent(this, EditTaskActivity::class.java).apply {
            putExtra("id", task.id)
            putExtra("title", task.title)
            putExtra("note", task.note)
            putExtra("position", task.position)
            putExtra("time_stamp", task.timeStamp)
            if (task.date != 0L) {
                putExtra("date", task.date)
            }
        }
        startActivity(intent)
    }

    fun setToolbarShadow(start: Float, end: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator.ofFloat(start, end).apply {
                addUpdateListener { updatedAnimation ->
                    toolbar?.elevation = updatedAnimation.animatedValue as Float
                }
                duration = 500
                start()
            }
        }
    }

    fun initScrollViewListener(scrollView: ScrollView) {
        var isShadowShown = false

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.scrollY > 0 && !isShadowShown) {
                setToolbarShadow(0f, 10f)
                isShadowShown = true
            } else if (scrollView.scrollY == 0 && isShadowShown) {
                setToolbarShadow(10f, 0f)
                isShadowShown = false
            }
        }
    }

    fun isHasPermissions(permission: String): Boolean {
        var result: Int

        for (currentPermission in arrayOf(permission)) {
            result = checkCallingOrSelfPermission(currentPermission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun showNoPermissionSnackbar(view: View, snackbarMessage: String, toastMessage: String, anchorView: View? = null) {
        val snackbar = Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_LONG)
                .setAction(R.string.permission_snackbar_button_settings) {
                    openApplicationSettings()
                    toastLong(toastMessage)
                }
        if (anchorView != null) {
            snackbar.anchorView = anchorView
        }
        snackbar.show()
    }

    fun requestPermissionWithRationale(view: View, message: String, permission: String, callback: PermissionRequestListener? = null, anchorView: View? = null) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.permission_snackbar_button_grant) {
                        if (callback != null) {
                            callback.onPermissionRequest()
                        } else requestPerms(permission)
                    }
            if (anchorView != null) {
                snackbar.anchorView = anchorView
            }
            snackbar.show()
        } else {
            if (callback != null) {
                callback.onPermissionRequest()
            } else requestPerms(permission)
        }
    }

    fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun hideKeyboard(editText: EditText) {
        editText.clearFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    interface PermissionRequestListener {
        fun onPermissionRequest()
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 123
    }
}