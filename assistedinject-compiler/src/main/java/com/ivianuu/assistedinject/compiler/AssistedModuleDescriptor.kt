package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

data class AssistedModuleDescriptor(
    val packageName: String,
    val moduleName: ClassName,
    val factories: List<AssistedFactoryDescriptor>,
    val isPublic: Boolean
)

data class AssistedFactoryDescriptor(
    val target: ClassName,
    val factory: ClassName,
    val superType: TypeName
)