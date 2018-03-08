package io.zenandroid.onlinego

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.NotificationUtils.Companion.updateNotification
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 24/11/2017.
 */
class NotificationsService : JobService() {
    companion object {
        val TAG = NotificationsService::class.java.simpleName
    }

    override fun onStartJob(job: JobParameters): Boolean {
        // Do some work here

        if(MainActivity.isInForeground) {
            return false
        }
        val connection = OGSServiceImpl()
        connection.loginWithToken()
                .andThen(connection.connectToNotifications())
                .take(10, TimeUnit.SECONDS)
                .filter { it.player_to_move == connection.uiConfig?.user?.id }
                .toList()
                .map { it.sortedWith(compareBy { it.id }) }
                .subscribe({
                    if (!MainActivity.isInForeground) {
                        updateNotification(this, it, connection.uiConfig?.user?.id)
                    }
                    connection.disconnect()
                    jobFinished(job, false)
                }, { e ->
                    Log.e(TAG, "Error when checking for notifications", e)
                    connection.disconnect()
                    jobFinished(job, true)
                })
        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return true // Answers the question: "Should this job be retried?"
    }

}