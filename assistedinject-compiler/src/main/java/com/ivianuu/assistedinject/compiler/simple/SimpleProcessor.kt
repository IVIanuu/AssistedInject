package com.ivianuu.assistedinject.compiler.simple

import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.common.base.Ascii
import com.google.common.base.Predicates
import com.google.common.collect.Collections2.transform
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimaps.filterKeys
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

/**
 * @author Manuel Wrage (IVIanuu)
 */
abstract class SimpleProcessor : AbstractProcessor() {

    private val steps by lazy(LazyThreadSafetyMode.NONE) { initSteps() }

    private lateinit var elements: Elements
    private lateinit var messager: Messager

    private val elementsDeferredBySteps = LinkedHashMultimap.create<ProcessingStep, ElementName>()
    private val deferredElementNamesBySteps =
        LinkedHashMultimap.create<ProcessingStep, ElementName>()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        elements = processingEnv.elementUtils
        messager = processingEnv.messager
        steps.forEach { it.init(processingEnv) }
    }

    override fun process(
        elements: Set<TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        // If this is the last round, report all of the missing elementUtils
        if (roundEnv.processingOver()) {
            steps.forEach { it.postRound(true) }
            postRound(roundEnv)
            reportMissingElements(
                deferredElementNamesBySteps.values()
                    .associate { it.name to it.getElement(this.elements) },
                elementsDeferredBySteps.values()
            )
        } else {
            steps.forEach { processStep(it, roundEnv) }
            steps.forEach { it.postRound(roundEnv.processingOver()) }
            postRound(roundEnv)
        }

        return false
    }

    override fun getSupportedAnnotationTypes() = getSupportedAnnotationClasses()
        .map { it.canonicalName }
        .toSet()

    private fun getSupportedAnnotationClasses() = steps
        .flatMap { it.annotations() }
        .toSet()

    protected abstract fun initSteps(): Iterable<ProcessingStep>

    protected open fun postRound(roundEnv: RoundEnvironment) {
    }

    private fun reportMissingElements(
        missingElements: Map<String, Element?>,
        missingElementNames: Collection<ElementName>
    ) {
        val allMissingElements = mutableMapOf<String, Element?>()
        allMissingElements.putAll(missingElements)

        missingElementNames
            .filterNot { missingElements.containsKey(it.name) }
            .forEach { allMissingElements[it.name] = it.getElement(elements) }

        missingElements.entries
            .forEach { (key, missingElement) ->
                if (missingElement != null) {
                    processingEnv
                        .messager
                        .printMessage(
                            Diagnostic.Kind.ERROR,
                            processingErrorMessage(
                                "this " + Ascii.toLowerCase(missingElement.kind.name)
                            ),
                            missingElement
                        )
                } else {
                    processingEnv
                        .messager
                        .printMessage(
                            Diagnostic.Kind.ERROR,
                            processingErrorMessage(key)
                        )
                }
            }
    }

    private fun processingErrorMessage(target: String) =
        "[${javaClass.simpleName}:MiscError] ${javaClass.canonicalName} was unable to process $target because not all of its dependencies could be resolved. Check for compilation errors or a circular dependency with generated code."

    private fun processStep(
        step: ProcessingStep,
        roundEnv: RoundEnvironment
    ) {
        val deferredElements = deferredElementNamesBySteps[step]
            .associate { it.name to it.getElement(this.elements) }
        deferredElementNamesBySteps.removeAll(step)
        val validElements = validElements(step, deferredElements, roundEnv)

        val stepElements = ImmutableSetMultimap.Builder<Class<out Annotation>, Element>()
            .putAll(indexByAnnotation(elementsDeferredBySteps.get(step), step.annotations()))
            .putAll(
                filterKeys(
                    validElements,
                    Predicates.`in`(step.annotations())
                )
            )
            .build()

        if (stepElements.isEmpty) {
            elementsDeferredBySteps.removeAll(step)
        } else {
            val rejectedElements = step.process(stepElements)
            elementsDeferredBySteps.replaceValues(
                step,
                transform(
                    rejectedElements
                ) { element -> ElementName.forAnnotatedElement(element!!) })
        }
    }

    private fun validElements(
        step: ProcessingStep,
        deferredElements: Map<String, Element?>,
        roundEnv: RoundEnvironment
    ): ImmutableSetMultimap<Class<out Annotation>, Element> {
        val deferredElementsByAnnotationBuilder =
            ImmutableSetMultimap.builder<Class<out Annotation>, Element>()

        for (deferredTypeElementEntry in deferredElements.entries) {
            val deferredElement = deferredTypeElementEntry.value
            if (deferredElement != null) {
                findAnnotatedElements(
                    deferredElement,
                    getSupportedAnnotationClasses(),
                    deferredElementsByAnnotationBuilder
                )
            } else {
                deferredElementNamesBySteps.put(
                    step, ElementName.forTypeName(deferredTypeElementEntry.key)
                )
            }
        }

        val deferredElementsByAnnotation = deferredElementsByAnnotationBuilder.build()

        val validElements = ImmutableSetMultimap.builder<Class<out Annotation>, Element>()

        // Look at the elementUtils we've found and the new elementUtils from this round and validate them.
        for (annotationClass in step.annotations()) {
            val elementsToValidate = roundEnv.getElementsAnnotatedWith(annotationClass)
                .union(deferredElementsByAnnotation.get(annotationClass))

            val (valid, deferred) = elementsToValidate.partition {
                step.validate(annotationClass, it)
            }

            validElements.putAll(annotationClass, valid)

            deferredElementNamesBySteps.putAll(step,
                deferred.map { ElementName.forAnnotatedElement(it) }
            )
        }

        return validElements.build()
    }

    private fun indexByAnnotation(
        annotatedElements: Set<ElementName>,
        annotationClasses: Set<Class<out Annotation>>
    ): ImmutableSetMultimap<Class<out Annotation>, Element> {
        val deferredElements = ImmutableSetMultimap.builder<Class<out Annotation>, Element>()

        annotatedElements
            .mapNotNull { it.getElement(elements) }
            .forEach { findAnnotatedElements(it, annotationClasses, deferredElements) }

        return deferredElements.build()
    }

    private fun findAnnotatedElements(
        element: Element,
        annotationClasses: Set<Class<out Annotation>>,
        annotatedElements: ImmutableSetMultimap.Builder<Class<out Annotation>, Element>
    ) {
        element.enclosedElements
            .filter { !it.kind.isClass && !it.kind.isInterface }
            .forEach { findAnnotatedElements(it, annotationClasses, annotatedElements) }

        (element as? ExecutableElement)
            ?.parameters
            ?.forEach {
                findAnnotatedElements(it, annotationClasses, annotatedElements)
            }

        annotationClasses
            .filter { isAnnotationPresent(element, it) }
            .forEach { annotatedElements.put(it, element) }
    }

    private data class ElementName(val kind: Kind, val name: String) {

        fun getElement(elements: Elements): Element? = if (kind == Kind.PACKAGE) {
            elements.getPackageElement(name)
        } else {
            elements.getTypeElement(name)
        }

        private enum class Kind {
            PACKAGE,
            TYPE
        }

        companion object {
            fun forPackageName(packageName: String) =
                ElementName(Kind.PACKAGE, packageName)

            fun forTypeName(typeName: String) = ElementName(Kind.TYPE, typeName)

            fun forAnnotatedElement(element: Element) =
                if (element.kind == ElementKind.PACKAGE) {
                    forPackageName((element as PackageElement).qualifiedName.toString())
                } else {
                    forTypeName(element.getEnclosingType().qualifiedName.toString())
                }
        }
    }

}

tailrec fun Element.getEnclosingType(): TypeElement =
    (this as? TypeElement) ?: enclosingElement.getEnclosingType()