package com.shist.data.workManager

import android.content.Context
import androidx.work.*
import com.shist.domain.DataRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// This class is instruction that WorkManager should do every 24 hours
class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val dataRepository: DataRepository by inject()

    override suspend fun doWork(): Result {

        try { // Update data periodically
            dataRepository.loadData()
        } catch (e: Throwable) {
            return Result.failure()
        }

        return Result.success()
    }

}