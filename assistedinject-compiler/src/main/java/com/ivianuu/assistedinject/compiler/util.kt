package com.ivianuu.assistedinject.compiler

import com.google.auto.common.MoreElements.isAnnotationPresent
import com.ivianuu.assistedinject.Assisted
import javax.lang.model.element.VariableElement

fun VariableElement.getFieldFromType() =
    enclosingElement.enclosingElement.enclosedElements
        .asSequence()
        .filterIsInstance<VariableElement>()
        .firstOrNull { it.simpleName.toString() == simpleName.toString() }

fun VariableElement.isAssisted() = isAnnotationPresent(this, Assisted::class.java)
        || getFieldFromType()
    ?.let { f -> isAnnotationPresent(f, Assisted::class.java) } ?: false