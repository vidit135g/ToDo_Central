package kotterknife

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <V : View> Activity.bindView(@IdRes id: Int): ReadOnlyProperty<Activity, V> =
    required(id, viewFinder)

fun <V : View> Fragment.bindView(@IdRes id: Int): ReadOnlyProperty<Fragment, V> =
    required(id, viewFinder)

fun <V : View> DialogFragment.bindView(@IdRes id: Int): ReadOnlyProperty<DialogFragment, V> =
    required(id, viewFinder)

private val Activity.viewFinder: Activity.(Int) -> View?
    get() = { findViewById(it) }

private val Fragment.viewFinder: Fragment.(Int) -> View?
    get() = { view?.findViewById(it) }

private val DialogFragment.viewFinder: DialogFragment.(Int) -> View?
    get() = { dialog?.findViewById(it) ?: view?.findViewById(it) }

private fun <T, V : View> required(id: Int, finder: T.(Int) -> View?) =
    Lazy { t: T, desc ->
        val view = t.finder(id)
        @Suppress("UNCHECKED_CAST")
        view as V? ?: throw IllegalStateException("View ID $id for '${desc.name}' not found.")
    }

private class Lazy<T, V>(private val initializer: (T, KProperty<*>) -> V) : ReadOnlyProperty<T, V> {
    private object EMPTY
    private var value: Any? = EMPTY

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        if (value === EMPTY) {
            value = initializer(thisRef, property)
        }
        @Suppress("UNCHECKED_CAST")
        return value as V
    }
}
