package com.ivianuu.assistedinject.compiler.simple

import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.SimpleAnnotationValueVisitor6

fun Element.validate(): Boolean {
    return when (this) {
        is PackageElement -> validateAnnotations(annotationMirrors)
        is TypeElement -> (validateBase()
                && typeParameters.validateAll()
                && interfaces.validateAll()
                && superclass.validate())
        is VariableElement -> validateBase()
        is ExecutableElement -> {
            return (validateBase()
                    && (defaultValue == null || defaultValue.validate(returnType))
                    && returnType.validate()
                    && thrownTypes.validateAll()
                    && typeParameters.validateAll()
                    && parameters.validateAll())
        }
        is TypeParameterElement -> validateBase() && bounds.validateAll()
        else -> true
    }
}

@JvmName("validateAllElements")
fun Iterable<Element>.validateAll() = all { it.validate() }

fun Element.validateBase() = (asType().validate()
        && validateAnnotations(annotationMirrors)
        && enclosedElements.validateAll())

@JvmName("validateAllTypes")
fun Iterable<TypeMirror>.validateAll() =
    all { it.validate() }

fun TypeMirror.validate(): Boolean {
    return when (this) {
        is ArrayType -> componentType.validate()
        is DeclaredType -> typeArguments.validateAll()
        is ErrorType -> false
        is WildcardType -> {
            val extendsBound = extendsBound
            val superBound = superBound
            return (extendsBound == null || extendsBound.validate())
                    && (superBound == null || superBound.validate())
        }
        is ExecutableType -> {
            parameterTypes.validateAll()
                    && returnType.validate()
                    && thrownTypes.validateAll()
                    && typeVariables.validateAll()
        }
        else -> true
    }
}

fun validateAnnotations(
    annotationMirrors: Iterable<AnnotationMirror>
) = annotationMirrors.all { it.validate() }

fun AnnotationMirror.validate() =
    annotationType.validate() && validateAnnotationValues(elementValues)

fun validateAnnotationValues(
    valueMap: Map<out ExecutableElement, AnnotationValue>
) = valueMap.all { it.value.validate(it.key.returnType) }

fun AnnotationValue.validate(
    expectedType: TypeMirror
): Boolean = accept(VALUE_VALIDATING_VISITOR, expectedType)

// todo remove
private val VALUE_VALIDATING_VISITOR =
    object : SimpleAnnotationValueVisitor6<Boolean, TypeMirror>() {
        override fun defaultAction(o: Any, expectedType: TypeMirror?): Boolean {
            return MoreTypes.isTypeOf(o.javaClass, expectedType!!)
        }

        override fun visitUnknown(av: AnnotationValue, expectedType: TypeMirror): Boolean {
            // just take the default action for the unknown
            return defaultAction(av, expectedType)
        }

        override fun visitAnnotation(a: AnnotationMirror, expectedType: TypeMirror): Boolean {
            return MoreTypes.equivalence().equivalent(
                a.annotationType,
                expectedType
            ) && a.validate()
        }

        override fun visitArray(
            values: List<AnnotationValue>,
            expectedType: TypeMirror
        ): Boolean {
            var expectedType = expectedType
            if (expectedType.kind != TypeKind.ARRAY) {
                return false
            }
            try {
                expectedType = MoreTypes.asArray(expectedType).componentType
            } catch (e: IllegalArgumentException) {
                return false // Not an array expected, ergo invalid.
            }

            for (value in values) {
                if (!value.accept(this, expectedType)) {
                    return false
                }
            }
            return true
        }

        override fun visitEnumConstant(
            enumConstant: VariableElement,
            expectedType: TypeMirror
        ): Boolean {
            return MoreTypes.equivalence().equivalent(
                enumConstant.asType(),
                expectedType
            ) && enumConstant.validate()
        }

        override fun visitType(type: TypeMirror, ignored: TypeMirror): Boolean {
            // We could check assignability here, but would require a Types instance. Since this
            // isn't really the sort of thing that shows up in a bad AST from upstream compilation
            // we ignore the expected type and just validate the type.  It might be wrong, but
            // it's valid.
            return type.validate()
        }

        override fun visitBoolean(b: Boolean, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Boolean.TYPE, expectedType)
        }

        override fun visitByte(b: Byte, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Byte.TYPE, expectedType)
        }

        override fun visitChar(c: Char, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(Character.TYPE, expectedType)
        }

        override fun visitDouble(d: Double, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Double.TYPE, expectedType)
        }

        override fun visitFloat(f: Float, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Float.TYPE, expectedType)
        }

        override fun visitInt(i: Int, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(Integer.TYPE, expectedType)
        }

        override fun visitLong(l: Long, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Long.TYPE, expectedType)
        }

        override fun visitShort(s: Short, expectedType: TypeMirror): Boolean {
            return MoreTypes.isTypeOf(java.lang.Short.TYPE, expectedType)
        }
    }