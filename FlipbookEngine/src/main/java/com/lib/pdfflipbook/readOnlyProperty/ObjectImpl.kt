package com.lib.pdfflipbook.readOnlyProperty

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ObjectImpl<T>(private val factory: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null) {
            value = factory()
        }
        return value!!
    }
}