package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

/**
 * @author Manuel Wrage (IVIanuu)
 */
data class AssistedModuleDescriptor(
    val packageName: String,
    val moduleName: ClassName,
    val factories: List<AssistedFactory>,
    val isPublic: Boolean
)

data class AssistedFactory(
    val target: ClassName,
    val factory: ClassName,
    val superType: TypeName
)