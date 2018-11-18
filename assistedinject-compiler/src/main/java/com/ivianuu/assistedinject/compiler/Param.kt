package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror

/**
 * @author Manuel Wrage (IVIanuu)
 */
data class Param(
    val type: TypeName,
    val name: String,
    val assisted: Boolean,
    val qualifiers: List<AnnotationMirror>
)