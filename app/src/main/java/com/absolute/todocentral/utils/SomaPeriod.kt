package com.absolute.todocentral.utils

import android.app.Activity
import android.content.Context
import androidx.annotation.StyleRes
import com.absolute.todocentral.R
import java.util.Calendar

/**
 * Resolves the active Soma dosha-period theme: either the one the user picked
 * in Settings, or the one matching the current hour ("auto", the default).
 * Same six windows and palettes as Notes Central, so the two apps stay in step:
 *
 * vata-dawn 2-6am · kapha-am 6-10am · pitta-day 10am-2pm ·
 * vata-pm 2-6pm · kapha-pm 6-10pm · pitta-night 10pm-2am
 */
object SomaPeriod {

    const val AUTO = "auto"

    const val VATA_DAWN = "vata-dawn"
    const val KAPHA_AM = "kapha-am"
    const val PITTA_DAY = "pitta-day"
    const val VATA_PM = "vata-pm"
    const val KAPHA_PM = "kapha-pm"
    const val PITTA_NIGHT = "pitta-night"

    val ORDER = arrayOf(VATA_DAWN, KAPHA_AM, PITTA_DAY, VATA_PM, KAPHA_PM, PITTA_NIGHT)

    const val PREF_KEY = "soma_period"

    /** The period actually in effect right now; never returns [AUTO] itself. */
    fun resolveId(): String {
        val saved = PreferenceHelper.getInstance().getString(PREF_KEY, AUTO)
        return if (saved == AUTO || saved.isNullOrEmpty()) {
            byHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        } else {
            saved
        }
    }

    fun byHour(hour: Int): String = when (hour) {
        in 2..5 -> VATA_DAWN
        in 6..9 -> KAPHA_AM
        in 10..13 -> PITTA_DAY
        in 14..17 -> VATA_PM
        in 18..21 -> KAPHA_PM
        else -> PITTA_NIGHT
    }

    /** The saved preference: a period id, or [AUTO]. */
    fun getPreference(): String =
        PreferenceHelper.getInstance().getString(PREF_KEY, AUTO) ?: AUTO

    fun setPreference(periodIdOrAuto: String) {
        PreferenceHelper.getInstance().putString(PREF_KEY, periodIdOrAuto)
    }

    @StyleRes
    fun themeResId(periodId: String): Int = when (periodId) {
        KAPHA_AM -> R.style.AppTheme_Soma_KaphaAm
        PITTA_DAY -> R.style.AppTheme_Soma_PittaDay
        VATA_PM -> R.style.AppTheme_Soma_VataPm
        KAPHA_PM -> R.style.AppTheme_Soma_KaphaPm
        PITTA_NIGHT -> R.style.AppTheme_Soma_PittaNight
        else -> R.style.AppTheme_Soma_VataDawn
    }

    fun labelResId(periodId: String): Int = when (periodId) {
        KAPHA_AM -> R.string.soma_period_kapha_am
        PITTA_DAY -> R.string.soma_period_pitta_day
        VATA_PM -> R.string.soma_period_vata_pm
        KAPHA_PM -> R.string.soma_period_kapha_pm
        PITTA_NIGHT -> R.string.soma_period_pitta_night
        else -> R.string.soma_period_vata_dawn
    }

    /**
     * Whether the period uses a light ground. Dialogs and pickers that still
     * only ship Light/Dark variants pick their style from this.
     */
    fun isLight(periodId: String = resolveId()): Boolean = when (periodId) {
        KAPHA_AM, PITTA_DAY, VATA_PM -> true
        else -> false
    }

    /** That period's ground colour, for previewing it outside its own theme. */
    fun bgColorResId(periodId: String): Int = when (periodId) {
        KAPHA_AM -> R.color.soma_kaphaAm_bg
        PITTA_DAY -> R.color.soma_pittaDay_bg
        VATA_PM -> R.color.soma_vataPm_bg
        KAPHA_PM -> R.color.soma_kaphaPm_bg
        PITTA_NIGHT -> R.color.soma_pittaNight_bg
        else -> R.color.soma_vataDawn_bg
    }

    /** That period's accent, for previewing it outside its own theme. */
    fun primaryColorResId(periodId: String): Int = when (periodId) {
        KAPHA_AM -> R.color.soma_kaphaAm_primary
        PITTA_DAY -> R.color.soma_pittaDay_primary
        VATA_PM -> R.color.soma_vataPm_primary
        KAPHA_PM -> R.color.soma_kaphaPm_primary
        PITTA_NIGHT -> R.color.soma_pittaNight_primary
        else -> R.color.soma_vataDawn_primary
    }

    /** Applies the active period's theme; call before setContentView. */
    fun apply(context: Context) {
        context.setTheme(themeResId(resolveId()))
    }
}

/**
 * Replaces the DressCode library's matchDressCode(): its theme setter is
 * internal to the library, so the period is applied directly instead.
 */
fun Activity.applySomaTheme() {
    setTheme(SomaPeriod.themeResId(SomaPeriod.resolveId()))
}
