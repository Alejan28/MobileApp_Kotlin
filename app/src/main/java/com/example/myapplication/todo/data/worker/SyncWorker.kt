package com.example.myapplication.todo.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.MyApplication
import com.example.myapplication.core.TAG

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MyApplication
        Log.d(TAG, "doWork started")
        app.container.itemRepository.sync()
        Log.d(TAG, "doWork finished")
        return Result.success()
    }
}