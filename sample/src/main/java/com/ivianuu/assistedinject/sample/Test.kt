package com.ivianuu.assistedinject.sample

import com.ivianuu.assistedinject.Assisted
import com.ivianuu.assistedinject.AssistedInject

/*
class MyService @Inject constructor(
    private val controllerFactory: MyControllerFactory
) {

}

class MyController @AssistedInject constructor(
    @Assisted private val service: MyService,
    private val helperFactory: MyHelperFactory
)

class MyHelper @AssistedInject constructor(
    @Assisted private val controller: MyController,
    private val app: App
)*/

class ParentType {

    class NestedType @AssistedInject constructor(
        @Assisted private val parent: ParentType,
        private val app: App
    ) {

    }

}