package com.ivianuu.assistedinject.compiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedInject
import com.ivianuu.assistedinject.AssistedModule
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AssistedModuleProcessingStep(private val processingEnv: ProcessingEnvironment) :
    BasicAnnotationProcessor.ProcessingStep {

    private var assistedModule: ClassName? = null
    private val factories = mutableListOf<AssistedFactory>()
    private var isPublic = false
    private var hasRun = false

    override fun annotations() =
        mutableSetOf(AssistedModule::class.java, AssistedInject.Factory::class.java)

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

        elementsByAnnotation[AssistedInject.Factory::class.java]
            .filterIsInstance<TypeElement>()
            .map {
                AssistedFactory(
                    ClassName.get(it.enclosingElement as TypeElement),
                    ClassName.bestGuess(
                        (it.enclosingElement as TypeElement).asType().toString() + "_AssistedFactory"
                    ),
                    ClassName.get(it)
                )
            }
            .forEach { factories.add(it) }

        postProcessing()

        return mutableSetOf()
    }

    fun postProcessing() {
        if (hasRun) return
        hasRun = true
        // no op
        if (factories.isEmpty()) return

        val assistedModule = assistedModule

        if (assistedModule == null) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "missing @AssistedModule annotated class"
            )
            return
        }

        val descriptor = AssistedModuleDescriptor(
            assistedModule.packageName(),
            ClassName.bestGuess("${assistedModule.simpleName()}_AssistedModule"),
            factories,
            isPublic
        )

        AssistedModuleGenerator(descriptor)
            .generate()
            .writeTo(processingEnv.filer)
    }
}