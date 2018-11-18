package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName

data class AssistedInjectDescriptor(
    val packageName: String,
    val target: ClassName,
    val targetParams: Set<Param>,
    val factoryName: ClassName,
    val superFactory: ClassName,
    val factoryMethod: String,
    val factoryParams: Set<Param>,
    val isPublic: Boolean
)