package com.ivianuu.assistedinject.compiler

import com.ivianuu.processingx.ProcessingEnvHolder
import com.ivianuu.processingx.ProcessingStep
import javax.annotation.processing.ProcessingEnvironment

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class BaseProcessingStep : ProcessingStep, ProcessingEnvHolder {

    override lateinit var processingEnv: ProcessingEnvironment

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        this.processingEnv = processingEnv
    }

}