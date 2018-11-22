package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import dagger.Binds
import dagger.Module
import javax.lang.model.element.Modifier

class AssistedModuleGenerator(private val descriptor: AssistedModuleDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.classBuilder(descriptor.moduleName)
            .addModifiers(Modifier.ABSTRACT)
            .apply {
                if (descriptor.isPublic) {
                    addModifiers(Modifier.PUBLIC)
                }
            }
            .addAnnotation(Module::class.java)
            .addMethods(bindsMethods())

        return JavaFile.builder(descriptor.packageName, type.build())
            .build()
    }

    fun bindsMethods() = descriptor.factories
        .map {
            MethodSpec.methodBuilder("bind_" + it.target.reflectionName().replace('.', '_'))
                .addModifiers(Modifier.ABSTRACT)
                .addAnnotation(Binds::class.java)
                .addParameter(it.factory, it.factory.simpleName().decapitalize())
                .returns(it.superType)
                .build()
        }
}