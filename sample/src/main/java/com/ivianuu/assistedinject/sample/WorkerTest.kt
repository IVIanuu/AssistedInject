package com.ivianuu.assistedinject.sample

import android.content.Context
import com.ivianuu.assistedinject.Assisted
import com.ivianuu.assistedinject.AssistedInject

open class Worker(context: Context, workerParams: WorkerParameters)

class WorkerParameters

interface InjectWorkerFactory {
    fun create(context: Context, workerParams: WorkerParameters): Worker
}

class MyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val app: App
) : Worker(context, workerParams) {
    @AssistedInject.Factory
    interface Factory : InjectWorkerFactory
}