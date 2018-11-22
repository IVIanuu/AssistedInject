package com.ivianuu.assistedinject.compiler

import com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations
import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.MoreElements.getAnnotationMirror
import com.google.auto.common.MoreElements.getLocalAndInheritedMethods
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedFactory
import com.ivianuu.assistedinject.AssistedInject
import com.ivianuu.assistedinject.compiler.simple.BaseProcessingStep
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import javax.inject.Qualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

class AssistedInjectProcessingStep : BaseProcessingStep() {

    override fun annotations() = setOf(AssistedInject::class.java)

    override fun validate(annotationClass: Class<out Annotation>, element: Element) = true

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        elementsByAnnotation[AssistedInject::class.java]
            .filterIsInstance<ExecutableElement>()
            .mapNotNull { createDescriptor(it) }
            .map { AssistedInjectGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createDescriptor(element: ExecutableElement): AssistedInjectDescriptor? {
        val type = (element.enclosingElement as TypeElement)

        val params = element.parameters
            .map {
                Param(
                    TypeName.get(it.asType()),
                    it.simpleName.toString(),
                    it.isAssisted(),
                    getAnnotatedAnnotations(it, Qualifier::class.java).toList()
                )
            }
            .toSet()

        val assistedParams = params.filter { it.assisted }

        val factoryAnnotation = getAnnotationMirror(type, AssistedFactory::class.java).orNull()

        val factoryTypeMirror = if (factoryAnnotation != null) {
            getAnnotationValue(factoryAnnotation, "clazz").value as TypeMirror
        } else {
            null
        }

        val factory =
            factoryTypeMirror?.let { elementUtils.getTypeElement(it.toString()) }

        val factoryDescriptor = if (factory != null) {
            val factoryMethods = getLocalAndInheritedMethods(
                factory, typeUtils, elementUtils
            ).filterNot { it.isDefault }

            if (factoryMethods.isEmpty()) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR, "factory interface has no methods", factory
                )
                return null
            } else if (factoryMethods.size > 1) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR, "factory interface has multiple methods", factory
                )
                return null
            }

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

            val factoryExecutable = typeUtils.asMemberOf(
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
                    Param(
                        TypeName.get(type),
                        ktName ?: element.simpleName.toString(),
                        true,
                        emptyList()
                    )
                }

            if (assistedParams.sortedBy { it.name } != factoryParams.sortedBy { it.name }) {
                val missingParams = assistedParams.filterNot { factoryParams.contains(it) }
                    .map { it.name to it.type.toString() }
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "factory method ${factoryMethod.simpleName} does not match the @AssistedInject type: missing params $missingParams",
                    element
                )
                return null
            }

            FactoryDescriptor(
                ClassName.bestGuess(factoryTypeMirror.toString()),
                factoryMethod.simpleName.toString(), factoryParams.toSet()
            )
        } else {
            val factoryName = type.className("Factory")
            val methodName = "create"

            val autoFactoryDescriptor = AutoFactoryDescriptor(
                ClassName.get(type).packageName(),
                factoryName,
                ClassName.get(type),
                methodName,
                assistedParams.toSet(),
                type.modifiers.contains(Modifier.PUBLIC)
            )

            AutoFactoryGenerator(autoFactoryDescriptor).generate()
                .writeTo(filer)

            FactoryDescriptor(factoryName, methodName, assistedParams.toSet())
        }

        return AssistedInjectDescriptor(
            ClassName.get(type).packageName(),
            ClassName.get(type),
            params,
            type.className("AssistedFactory"),
            factoryDescriptor.factoryName,
            factoryDescriptor.methodName,
            factoryDescriptor.params,
            type.modifiers.contains(Modifier.PUBLIC)
        )
    }
}