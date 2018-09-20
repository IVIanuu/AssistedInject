/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.assistedinject.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

/**
 * @author Manuel Wrage (IVIanuu)
 */
data class AssistedInjectDescriptor(
    val element: ExecutableElement,
    val packageName: String,
    val type: ClassName,
    val factoryName: ClassName,
    val params: Set<AssistedInjectParam>,
    val isPublic: Boolean
)

data class AssistedInjectParam(
    val element: VariableElement,
    val type: TypeName,
    val name: String,
    val assisted: Boolean,
    val qualifiers: List<AnnotationMirror>
)