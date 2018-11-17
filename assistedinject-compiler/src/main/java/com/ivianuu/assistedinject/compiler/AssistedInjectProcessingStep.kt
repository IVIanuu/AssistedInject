package com.ivianuu.assistedinject.compiler

import com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements.getLocalAndInheritedMethods
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedInject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Qualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.tools.Diagnostic

class AssistedInjectProcessingStep(
    private val processingEnv: ProcessingEnvironment
) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = mutableSetOf(AssistedInject::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        elementsByAnnotation[AssistedInject::class.java]
            .filterIsInstance<ExecutableElement>()
            .mapNotNull { createDescriptor(it) }
            .map { AssistedInjectGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(processingEnv.filer) }

        return mutableSetOf()
    }

    private fun createDescriptor(element: ExecutableElement): AssistedInjectDescriptor? {
        val type = (element.enclosingElement as TypeElement)

        val params = element.parameters
            .map {
                AssistedInjectDescriptor.Param(
                    TypeName.get(it.asType()),
                    it.simpleName.toString(),
                    it.isAssisted(),
                    getAnnotatedAnnotations(it, Qualifier::class.java).toList()
                )
            }
            .toSet()

        val factories = type.enclosedElements
            .filter { isAnnotationPresent(it, AssistedInject.Factory::class.java) }

        val factory = when {
            factories.size == 1 -> factories.first()
            factories.size > 1 -> {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Only one @AssistedInject.Factory interface is allowed per class"
                )
                return null
            }
            else -> {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@AssistedInject annotated classes must also have a nested @AssistedInject.Factory annotated factory interface"
                )
                return null
            }
        }

        val factoryTypeMirror = factory.asType()
        val factoryType = processingEnv.elementUtils.getTypeElement(factoryTypeMirror.toString())

        val factoryMethods = getLocalAndInheritedMethods(
            factoryType, processingEnv.typeUtils, processingEnv.elementUtils
        ).filterNot { it.isDefault }

        if (factoryMethods.isEmpty()) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR, "factory interface has no methods", factory
            )
            return null
        } else if (factoryMethods.size > 1) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR, "factory interface has multiple methods", factory
            )
            return null
        }

        val assistedParams = params.filter { it.assisted }

        val factoryMethod = factoryMethods.first()

        val factoryExecutable = processingEnv.typeUtils.asMemberOf(
            factoryType.asType() as DeclaredType,
            factoryMethod
        ) as ExecutableType
        val factoryParams = factoryMethod.parameters
            .zip(factoryExecutable.parameterTypes) { element, mirror ->
                element.simpleName.toString() to mirror
            }

        val factoryMethodValid = assistedParams
            .mapIndexed { i, p -> p to factoryParams[i] }
            .all { (assistedParam, param) ->
                assistedParam.type == TypeName.get(param.second)
                        && assistedParam.name == param.first
            }

        if (!factoryMethodValid) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "factory method ${factoryMethod.simpleName} does not match the @AssistedInject type",
                element
            )
            return null
        }

        return AssistedInjectDescriptor(
            ClassName.get(type).packageName(),
            ClassName.get(type),
            ClassName.bestGuess("${type.simpleName}_AssistedFactory"),
            ClassName.bestGuess(factoryTypeMirror.toString()),
            factoryMethod.simpleName.toString(),
            params,
            type.modifiers.contains(Modifier.PUBLIC)
        )
    }
}