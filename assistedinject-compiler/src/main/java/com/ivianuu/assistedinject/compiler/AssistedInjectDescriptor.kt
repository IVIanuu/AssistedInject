package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror

data class AssistedInjectDescriptor(
    val packageName: String,
    val target: ClassName,
    val factoryName: ClassName,
    val superFactory: ClassName,
    val functionName: String,
    val params: Set<Param>,
    val isPublic: Boolean
) {
    data class Param(
        val type: TypeName,
        val name: String,
        val assisted: Boolean,
        val qualifiers: List<AnnotationMirror>
    )
}