package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import kotlin.reflect.KProperty

abstract class Binding {
    val delegates = mutableMapOf<String, RenderProp<out Any>>()
    var isInflate = false
    open val afterInflate: (() -> Unit)? = null
    open fun onFinishInflate() {
        if (!isInflate) {
            afterInflate?.invoke()
            isInflate = true
        }
    }
    abstract fun bind(data:IViewModelState)
    open fun saveUi(outState: Bundle) {}
    open fun restoreUi(savedState: Bundle?) {}


    fun <A, B> dependsOn(
        vararg fields: KProperty<*>,
        onChange: (A, B) -> Unit
    ) {
        check(fields.size == 2) { "Names size should be 2, current: ${fields.size}" }

        val names = fields.map { it.name }

        names.forEach {
            delegates[it]?.addListener {
                onChange(
                    delegates[names[0]]?.value as A,
                    delegates[names[1]]?.value as B
                )
            }
        }
    }

    fun <A, B, C, D> dependsOn(
        vararg fields: KProperty<*>,
        onChange: (A, B, C, D) -> Unit
    ) {
        check(fields.size == 4) { "Names size should be 4, current: ${fields.size}" }

        val names = fields.map { it.name }

        names.forEach {
            delegates[it]?.addListener {
                onChange(
                    delegates[names[0]]?.value as A,
                    delegates[names[1]]?.value as B,
                    delegates[names[2]]?.value as C,
                    delegates[names[3]]?.value as D
                )
            }
        }
    }

    fun rebind() {
        delegates.forEach {
            it.value.bind()
        }
    }
}