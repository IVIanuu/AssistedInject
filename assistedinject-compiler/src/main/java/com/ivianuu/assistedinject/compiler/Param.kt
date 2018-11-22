package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror

data class Param(
    val type: TypeName,
    val name: String,
    val assisted: Boolean,
    val qualifiers: Set<AnnotationMirror>
)