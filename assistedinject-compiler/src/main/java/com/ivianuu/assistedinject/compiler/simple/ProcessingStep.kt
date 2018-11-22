package com.ivianuu.assistedinject.compiler.simple

import com.google.common.collect.SetMultimap
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface ProcessingStep {

    fun init(processingEnv: ProcessingEnvironment) {
    }

    fun annotations(): Set<Class<out Annotation>>

    fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): Set<Element>

    fun postRound(processingOver: Boolean) {

    }

}