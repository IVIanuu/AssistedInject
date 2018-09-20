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

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.Assisted
import com.ivianuu.assistedinject.AssistedInject
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Qualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AssistedInjectProcessingStep(
    private val processingEnv: ProcessingEnvironment
) : BasicAnnotationProcessor.ProcessingStep {

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        elementsByAnnotation[AssistedInject::class.java]
            .asSequence()
            .filterIsInstance<ExecutableElement>()
            .map { createDescriptor(it) }
            .filterNotNull()
            .map { AssistedInjectGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(processingEnv.filer) }

        return mutableSetOf()
    }

    override fun annotations(): MutableSet<out Class<out Annotation>> =
            mutableSetOf(AssistedInject::class.java)

    private fun createDescriptor(element: ExecutableElement): AssistedInjectDescriptor? {
        val type = element.enclosingElement as TypeElement

        val params = element.parameters
            .asSequence()
            .map { param ->
                val name = param.simpleName.toString()

                // find the field for the constructor element
                // to check if a @Assisted annotation is present
                val field = type.enclosedElements
                    .asSequence()
                    .filterIsInstance<VariableElement>()
                    .firstOrNull { it.simpleName.toString() == name }

                if (field == null) {
                    // todo notify error
                }

                AssistedInjectParam(
                    param,
                    ClassName.get(param.asType()),
                    name,
                    MoreElements.isAnnotationPresent(field, Assisted::class.java),
                    AnnotationMirrors.getAnnotatedAnnotations(param, Qualifier::class.java).toList()
                )
            }
            .toSet()

        val factoryName = ClassName.bestGuess(
            type.asType().toString() + "Factory"
        )

        return AssistedInjectDescriptor(element, factoryName.packageName(),
            ClassName.get(type), factoryName, params, type.modifiers.contains(Modifier.PUBLIC))
    }
}