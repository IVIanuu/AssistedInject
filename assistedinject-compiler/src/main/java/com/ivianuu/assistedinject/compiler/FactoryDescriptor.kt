package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName

data class FactoryDescriptor(
    val factoryName: ClassName,
    val methodName: String,
    val params: Set<Param>
)