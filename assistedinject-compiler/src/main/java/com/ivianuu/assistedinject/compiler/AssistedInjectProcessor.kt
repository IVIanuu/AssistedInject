/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.assistedinject.compiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import javax.annotation.processing.Processor

/**
 * Assisted inject processor
 */
@AutoService(Processor::class)
class AssistedInjectProcessor : BasicAnnotationProcessor() {

    private val assistedModuleStep by lazy {
        AssistedModuleProcessingStep(processingEnv)
    }

    override fun initSteps() =
        mutableSetOf(
            AssistedInjectProcessingStep(processingEnv),
            assistedModuleStep
        )

}