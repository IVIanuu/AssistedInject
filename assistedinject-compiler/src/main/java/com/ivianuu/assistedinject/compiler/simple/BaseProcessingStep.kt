package com.ivianuu.assistedinject.compiler.simple

import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class BaseProcessingStep : ProcessingStep {

    protected lateinit var processingEnv: ProcessingEnvironment

    protected val elementUtils: Elements get() = processingEnv.elementUtils
    protected val filer: Filer get() = processingEnv.filer
    protected val messager: Messager get() = processingEnv.messager
    protected val typeUtils: Types get() = processingEnv.typeUtils

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        this.processingEnv = processingEnv
    }

}