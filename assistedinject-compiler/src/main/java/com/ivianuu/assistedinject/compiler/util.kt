package com.ivianuu.assistedinject.compiler

import com.ivianuu.assistedinject.Assisted
import com.ivianuu.processingx.getPackage
import com.ivianuu.processingx.hasAnnotation
import com.squareup.javapoet.ClassName
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

fun VariableElement.getFieldFromType() =
    enclosingElement.enclosingElement.enclosedElements
        .asSequence()
        .filterIsInstance<VariableElement>()
        .firstOrNull { it.simpleName.toString() == simpleName.toString() }

fun VariableElement.isAssisted() = hasAnnotation<Assisted>()
        || getFieldFromType()?.hasAnnotation<Assisted>() ?: false

fun TypeElement.className(suffix: String): ClassName =
    ClassName.get(getPackage().qualifiedName.toString(), baseClassName() + suffix)

private fun TypeElement.baseClassName() = qualifiedName.toString().substring(
    getPackage().qualifiedName.toString().length + 1
).replace('.', '_')