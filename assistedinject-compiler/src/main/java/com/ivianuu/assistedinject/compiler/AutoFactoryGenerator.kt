package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.NotNull
import javax.lang.model.element.Modifier

class AutoFactoryGenerator(private val descriptor: AutoFactoryDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.interfaceBuilder(descriptor.factoryName)
            .apply {
                if (descriptor.isPublic) {
                    addModifiers(Modifier.PUBLIC)
                }
            }
            .addMethod(createFunction())

        return JavaFile.builder(descriptor.packageName, type.build())
            .build()
    }

    private fun createFunction() = MethodSpec.methodBuilder(descriptor.methodName)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .returns(descriptor.target)
        .apply {
            descriptor.params.forEach {
                addParameter(it.type, it.name)
            }
        }
        .addAnnotation(NotNull::class.java)
        .build()
}