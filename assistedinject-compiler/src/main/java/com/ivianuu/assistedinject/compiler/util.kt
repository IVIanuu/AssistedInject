package com.ivianuu.assistedinject.compiler

import com.google.auto.common.MoreElements.getPackage
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.ivianuu.assistedinject.Assisted
import com.squareup.javapoet.ClassName
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

fun VariableElement.getFieldFromType() =
    enclosingElement.enclosingElement.enclosedElements
        .asSequence()
        .filterIsInstance<VariableElement>()
        .firstOrNull { it.simpleName.toString() == simpleName.toString() }

fun VariableElement.isAssisted() = isAnnotationPresent(this, Assisted::class.java)
        || getFieldFromType()
    ?.let { f -> isAnnotationPresent(f, Assisted::class.java) } ?: false

fun TypeElement.className(suffix: String) =
    ClassName.get(packageName(), baseClassName() + suffix)

private fun TypeElement.baseClassName() = qualifiedName.toString().substring(
    packageName().length + 1
).replace('.', '_')

fun Element.packageName() =
    getPackage(enclosingElement).qualifiedName.toString()