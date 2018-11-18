package com.ivianuu.assistedinject.compiler

import com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements.getLocalAndInheritedMethods
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedInject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
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
            factories.size == 1 -> factories.first() as TypeElement
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

        val factoryMethods = getLocalAndInheritedMethods(
            factory, processingEnv.typeUtils, processingEnv.elementUtils
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

        val ktMeta = factoryMethod.enclosingElement.kotlinMetadata

        // find the kt parameter names
        // because for some reason interface parameter names
        // will be renamed to var1, var2.. when compiled and used in a different module
        val ktFactoryMethodParameterNames = if (ktMeta is KotlinClassMetadata) {
            ktMeta.data.classProto.getFunction(0)
                .valueParameterList
                .map { ktMeta.data.nameResolver.getString(it.name) }
        } else {
            null
        }

        val factoryExecutable = processingEnv.typeUtils.asMemberOf(
            factory.asType() as DeclaredType,
            factoryMethod
        ) as ExecutableType

        val factoryParams = factoryMethod.parameters
            .mapIndexed { i, param ->
                Triple(
                    param,
                    factoryExecutable.parameterTypes[i],
                    ktFactoryMethodParameterNames?.get(i)
                )
            }
            .map { (element, type, ktName) ->
                AssistedInjectDescriptor.Param(
                    TypeName.get(type),
                    ktName ?: element.simpleName.toString(),
                    true,
                    emptyList()
                )
            }

        if (assistedParams.sortedBy { it.name } != factoryParams.sortedBy { it.name }) {
            val missingParams = assistedParams.filterNot { factoryParams.contains(it) }
                .map { it.name to it.type.toString() }
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "factory method ${factoryMethod.simpleName} does not match the @AssistedInject type: missing params $missingParams",
                element
            )
            return null
        }

        return AssistedInjectDescriptor(
            ClassName.get(type).packageName(),
            ClassName.get(type),
            params,
            ClassName.bestGuess("${type.simpleName}_AssistedFactory"),
            ClassName.bestGuess(factoryTypeMirror.toString()),
            factoryMethod.simpleName.toString(),
            factoryParams.toSet(),
            type.modifiers.contains(Modifier.PUBLIC)
        )
    }
}