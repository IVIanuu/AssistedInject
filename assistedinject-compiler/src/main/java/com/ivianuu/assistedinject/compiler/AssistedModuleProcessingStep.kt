package com.ivianuu.assistedinject.compiler

import com.google.common.collect.SetMultimap
import com.ivianuu.assistedinject.AssistedFactory
import com.ivianuu.assistedinject.AssistedInject
import com.ivianuu.assistedinject.AssistedModule
import com.ivianuu.processingx.asJavaClassName
import com.ivianuu.processingx.asJavaTypeName
import com.ivianuu.processingx.elementUtils
import com.ivianuu.processingx.filer
import com.ivianuu.processingx.getAnnotationMirror
import com.ivianuu.processingx.getAnnotationMirrorOrNull
import com.ivianuu.processingx.getAsType
import com.ivianuu.processingx.getAsTypeList
import com.ivianuu.processingx.getPackage
import com.ivianuu.processingx.messager
import com.squareup.javapoet.ClassName
import dagger.Module
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AssistedModuleProcessingStep : BaseProcessingStep() {

    private var moduleGenerated = false
    private var assistedModuleName: ClassName? = null
    private var generatedModuleName: ClassName? = null
    private val factories = mutableListOf<AssistedFactoryDescriptor>()

    override fun annotations() =
        setOf(AssistedModule::class.java, AssistedInject::class.java)

    override fun validate(annotationClass: Class<out Annotation>, element: Element) = true

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        elementsByAnnotation[AssistedInject::class.java]
            .filterIsInstance<ExecutableElement>()
            .map { element ->
                val target = (element.enclosingElement as TypeElement)

                val assistedFactoryAnnotation = target.getAnnotationMirrorOrNull<AssistedFactory>()

                if (assistedFactoryAnnotation != null) {
                    val factory =
                        assistedFactoryAnnotation.getAsType("clazz")

                    AssistedFactoryDescriptor(
                        target.asJavaClassName(),
                        target.className("AssistedFactory"),
                        factory.asJavaTypeName()
                    )
                } else {
                    AssistedFactoryDescriptor(
                        target.asJavaClassName(),
                        target.className("AssistedFactory"),
                        target.className("Factory")
                    )
                }
            }
            .forEach { factories.add(it) }

        elementsByAnnotation[AssistedModule::class.java]
            .filterIsInstance<TypeElement>()
            .forEach {
                if (moduleGenerated) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "only one module should be annotated with @AssistedModule"
                    )
                    return@forEach
                }

                assistedModuleName = it.asJavaClassName()

                val isPublic = it.modifiers.contains(Modifier.PUBLIC)
                val moduleAnnotation = it.getAnnotationMirrorOrNull<Module>()

                if (moduleAnnotation == null) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@AssistedModules must also have @Module annotation"
                    )
                    return@forEach
                }

                val assistedModuleName = it.className("_AssistedModule")
                    .also { generatedModuleName = it }

                val descriptor = AssistedModuleDescriptor(
                    it.getPackage().qualifiedName.toString(),
                    assistedModuleName,
                    factories,
                    isPublic
                )

                AssistedModuleGenerator(descriptor)
                    .generate()
                    .writeTo(filer)

                moduleGenerated = true
            }

        return emptySet()
    }

    override fun postRound(processingOver: Boolean) {
        if (!processingOver) return

        if (!moduleGenerated && factories.isNotEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "missing @AssistedModule annotated class"
            )

            return
        }

        val assistedModule = elementUtils.getTypeElement(assistedModuleName!!.toString())

        val moduleAnnotation = assistedModule.getAnnotationMirror<Module>()

        val includesEntries = moduleAnnotation.getAsTypeList("includes")

        if (!includesEntries.map { it.toString() }.contains(generatedModuleName!!.toString())) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@AssistedModule annotated modules must include the generated assisted module",
                assistedModule
            )
        }
    }

}