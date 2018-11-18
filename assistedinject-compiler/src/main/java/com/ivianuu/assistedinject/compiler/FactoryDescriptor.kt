package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName

/**
 * @author Manuel Wrage (IVIanuu)
 */
data class FactoryDescriptor(
    val factoryName: ClassName,
    val methodName: String,
    val params: Set<Param>
)