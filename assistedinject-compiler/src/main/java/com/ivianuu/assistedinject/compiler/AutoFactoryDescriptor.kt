package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName

data class AutoFactoryDescriptor(
    val packageName: String,
    val factoryName: ClassName,
    val target: ClassName,
    val methodName: String,
    val params: Set<Param>,
    val isPublic: Boolean
)