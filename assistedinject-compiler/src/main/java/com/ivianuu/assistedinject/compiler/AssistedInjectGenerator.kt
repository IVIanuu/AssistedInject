package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.NotNull
import javax.inject.Inject
import javax.inject.Provider
import javax.lang.model.element.Modifier

class AssistedInjectGenerator(private val descriptor: AssistedInjectDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.classBuilder(descriptor.factoryName)
            .addModifiers(Modifier.FINAL)
            .addFields(fields())
            .addMethod(constructor())
            .addMethod(create())
            .addSuperinterface(descriptor.superFactory)

        if (descriptor.isPublic) {
            type.addModifiers(Modifier.PUBLIC)
        }

        return JavaFile.builder(descriptor.packageName, type.build())
            .build()
    }

    private fun constructor(): MethodSpec {
        val params =
            descriptor.targetParams.asSequence()
                .filterNot { it.assisted }
                .toSet()

        return MethodSpec.constructorBuilder()
            .apply { if (descriptor.isPublic) addModifiers(Modifier.PUBLIC) }
            .addAnnotation(Inject::class.java)
            .addParameters(
                params
                    .asSequence()
                    .map {
                        ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Provider::class.java), it.type),
                            it.name
                        )
                            .addAnnotation(NotNull::class.java)
                            .addAnnotations(it.qualifiers.map { AnnotationSpec.get(it) })
                            .build()
                    }
                    .toList()
            )
            .addCode(
                CodeBlock.builder()
                    .apply {
                        params.forEach {
                            addStatement("this.${it.name} = ${it.name}")
                        }
                    }
                    .build()
            )
            .build()
    }

    private fun fields(): Set<FieldSpec> {
        return descriptor.targetParams
            .asSequence()
            .filterNot { it.assisted }
            .map {
                FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(Provider::class.java),
                        it.type
                    ), it.name
                )
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
            }
            .toSet()
    }

    private fun create(): MethodSpec {
        val statement = "return new \$T(${
        descriptor.targetParams
            .asSequence()
            .map { if (it.assisted) it.name else "${it.name}.get()" }
            .joinToString(", ")
        })"

        return MethodSpec.methodBuilder(descriptor.factoryMethod)
            .addAnnotation(NotNull::class.java)
            .apply { if (descriptor.isPublic) addModifiers(Modifier.PUBLIC) }
            .returns(descriptor.target)
            .addParameters(
                descriptor.factoryParams
                    .asSequence()
                    .map { ParameterSpec.builder(it.type, it.name).build() }
                    .toSet()
            )
            .addStatement(statement, descriptor.target)
            .build()
    }
}