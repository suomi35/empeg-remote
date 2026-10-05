package com.chasinglemons.empeg

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button

class WidgetConfigure : Activity() {
    private val self: Context = this
    private var appWidgetId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get the appWidgetId of the appWidget being configured
        val launchIntent = intent
        val extras = launchIntent.extras
        appWidgetId = extras!!.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        // set the result for cancel first
        val cancelResultValue = Intent()
        cancelResultValue.putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(RESULT_CANCELED, cancelResultValue)
        setContentView(R.layout.widget_configure)

        // the OK button
        val ok = findViewById<View>(R.id.button_ok) as Button
        ok.setOnClickListener { // fire an update to display initial state of the widget
            val updatepending: PendingIntent =
                Widget.Companion.makeControlPendingIntent(
                    self,
                    WidgetService.Companion.UPDATE, appWidgetId
                )
            try {
                updatepending.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
            // change the result to OK
            val resultValue = Intent()
            resultValue.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                appWidgetId
            )
            setResult(RESULT_OK, resultValue)
            finish()
        }

        // cancel button
        val cancel = findViewById<View>(R.id.button_cancel) as Button
        cancel.setOnClickListener { finish() }
    }
}
