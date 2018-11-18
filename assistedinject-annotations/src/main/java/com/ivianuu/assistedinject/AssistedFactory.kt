package com.ivianuu.assistedinject

import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Target(AnnotationTarget.CLASS)
annotation class AssistedFactory(val clazz: KClass<*>)