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

package com.ivianuu.assistedinject.sample

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import com.ivianuu.assistedinject.Assisted
import com.ivianuu.assistedinject.AssistedInject
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Module
abstract class ViewModelFactoryModule {

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModelFactory(factory: MainViewModel.Factory): KeyViewModelFactory<*>

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModelFactory(factory: HomeViewModel.Factory): KeyViewModelFactory<*>
}

interface KeyViewModelFactory<K : Parcelable> {
    fun create(key: K): ViewModel
}

class MainViewModel @AssistedInject constructor(
    @Assisted private val key: Bundle,
    @AppContext private val context: Context,
    private val myDep1: MyDep1,
    private val myDep2: MyDep2
) : ViewModel() {

    @AssistedInject.Factory
    interface Factory : KeyViewModelFactory<Bundle>
}

class HomeViewModel @AssistedInject constructor(
    @Assisted private val key: Bundle,
    @AppContext private val context: Context,
    private val myDep1: MyDep1,
    private val myDep2: MyDep2
) : ViewModel() {

    @AssistedInject.Factory
    interface Factory : KeyViewModelFactory<Bundle>
}