package com.vladpen

import android.app.Activity
import android.os.Build
import android.view.View
import android.widget.ImageButton
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.vladpen.cams.R

object Alert {
    private const val WORK_NAME = "CamsAlertWork"
    private const val ENABLE_FILE_NAME = "alert.bin"
    private var isAvailable: Boolean? = null

    fun init(activity: Activity, button: ImageButton) {
        if (isAvailable == null)
            this.checkAvailability()
        if (isAvailable == false)
            return

        button.visibility = View.VISIBLE
        button.setOnClickListener {
            this.toggle(activity, button)
        }

        if (Utils.getOption(this.ENABLE_FILE_NAME, 1) == 0) {
            this.cancelWork(activity)
            button.setImageResource(R.drawable.ic_outline_alert_off_24)
        } else {
            this.enqueueWork(activity)
        }
    }

    private fun toggle(activity: Activity, button: ImageButton) {
        if (Utils.getOption(this.ENABLE_FILE_NAME, 1) == 1) {
            Utils.saveOption(this.ENABLE_FILE_NAME, 0)
            button.setImageResource(R.drawable.ic_outline_alert_off_24)
            this.cancelWork(activity)
        } else {
            Utils.saveOption(this.ENABLE_FILE_NAME, 1)
            button.setImageResource(R.drawable.ic_outline_alert_on_24)
            this.enqueueWork(activity)
        }
    }

    private fun enqueueWork(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33 // Android 13 API 33 (T)
                && !NotificationManagerCompat.from(activity).areNotificationsEnabled())
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val alertRequest = OneTimeWorkRequestBuilder<AlertWork>().setConstraints(constraints)
        if (Build.VERSION.SDK_INT >= 31) // Android 11 API 31 (S)
            alertRequest.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        WorkManager.getInstance(activity).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.REPLACE, alertRequest.build())
    }

    private fun cancelWork(activity: Activity) {
        WorkManager.getInstance(activity).cancelUniqueWork(WORK_NAME)
    }

    fun checkAvailability() {
        isAvailable = false
        AlertWork.clearLastTimes()
        val streams = StreamData.getAll()
        if (streams.isEmpty())
            return
        for (i in streams.indices) {
            if (streams[i].alert == true && streams[i].sftp != "") {
                isAvailable = true
                break
            }
        }
    }
}
