package com.chasinglemons.empeg

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock

class Widget : AppWidgetProvider() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            setAlarm(context, appWidgetId, -1)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, WidgetService::class.java))
        super.onDisabled(context)
    }

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            setAlarm(context, appWidgetId, UPDATE_RATE)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        //update rate in milliseconds
        var UPDATE_RATE: Int = 1000

        fun setAlarm(context: Context, appWidgetId: Int, updateRate: Int) {
            val newPending =
                makeControlPendingIntent(context, WidgetService.Companion.UPDATE, appWidgetId)
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (updateRate >= 0) {
                alarms.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    updateRate.toLong(),
                    newPending
                )
            } else {
                // on a negative updateRate stop the refreshing
                alarms.cancel(newPending)
            }
        }

        fun makeControlPendingIntent(
            context: Context?,
            command: String,
            appWidgetId: Int
        ): PendingIntent {
            val active = Intent(context, WidgetService::class.java)
            active.setAction(command)
            active.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // this Uri data is to make the PendingIntent unique, so it wont be updated by FLAG_UPDATE_CURRENT
            // so if there are multiple widget instances they wont override each other
            val data = Uri.withAppendedPath(
                Uri.parse("empegwidget://widget/id/#$command$appWidgetId"),
                appWidgetId.toString()
            )
            active.setData(data)
            return (PendingIntent.getService(context, 0, active, PendingIntent.FLAG_UPDATE_CURRENT))
        }
    }
}

