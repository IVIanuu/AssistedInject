package com.ivianuu.assistedinject.compiler

import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements.getAnnotationMirror
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedInject
import com.ivianuu.assistedinject.AssistedModule
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AssistedModuleProcessingStep(private val processingEnv: ProcessingEnvironment) :
    BasicAnnotationProcessor.ProcessingStep {

    private var assistedModule: ClassName? = null
    private val factories = mutableListOf<AssistedFactoryDescriptor>()
    private var isPublic = false

    override fun annotations() =
        mutableSetOf(AssistedModule::class.java, AssistedInject::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        elementsByAnnotation[AssistedModule::class.java]
            .filterIsInstance<TypeElement>()
            .forEach {
                val currentAssistedModule = assistedModule
                if (currentAssistedModule != null) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "only one module should be annotated with @AssistedModule"
                    )
                }

                assistedModule = ClassName.get(it)
                isPublic = it.modifiers.contains(Modifier.PUBLIC)
            }

        elementsByAnnotation[AssistedInject::class.java]
            .filterIsInstance<ExecutableElement>()
            .map { element ->
                val target = (element.enclosingElement as TypeElement)
                val assistedFactoryAnnotation = getAnnotationMirror(
                    target,
                    com.ivianuu.assistedinject.AssistedFactory::class.java
                ).orNull()

                if (assistedFactoryAnnotation != null) {
                    val factory =
                        getAnnotationValue(assistedFactoryAnnotation, "clazz").value as TypeMirror

                    AssistedFactoryDescriptor(
                        ClassName.get(target),
                        ClassName.bestGuess(target.asType().toString() + "_AssistedFactory"),
                        ClassName.bestGuess(factory.toString())
                    )
                } else {
                    AssistedFactoryDescriptor(
                        ClassName.get(target),
                        ClassName.bestGuess(target.asType().toString() + "_AssistedFactory"),
                        ClassName.bestGuess(target.asType().toString() + "Factory")
                    )
                }
            }
            .forEach { factories.add(it) }

        // no op
        if (factories.isNotEmpty()) {
            val assistedModule = assistedModule

            if (assistedModule != null) {
                val descriptor = AssistedModuleDescriptor(
                    assistedModule.packageName(),
                    ClassName.bestGuess("${assistedModule.simpleName()}_AssistedModule"),
                    factories,
                    isPublic
                )

                AssistedModuleGenerator(descriptor)
                    .generate()
                    .writeTo(processingEnv.filer)
            } else {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "missing @AssistedModule annotated class"
                )
            }
        }

        return mutableSetOf()
    }

}