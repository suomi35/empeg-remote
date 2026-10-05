package com.chasinglemons.empeg

import org.apache.http.client.HttpClient

class WidgetService : android.app.Service() {
    var empegCounter: String = "35"
    var lastCounter: String = "0"
    var thirdCounter: String = "00"
    var topDisplay: String? = null
    var bottomDisplay: String? = null
    var config: SharedPreferences? = null
    var runOnce: Boolean = false
    var isPaused: Boolean = false
    var playerIP: String? = null
    var remoteView: RemoteViews? = null
    var whichWidget: Int = 0

    override fun onStart(intent: android.content.Intent, startId: Int) {
        val command = intent.action
        val appWidgetId = intent.extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)

        config = PreferenceManager.getDefaultSharedPreferences(this)
        playerIP = config.getString("activeEmpegIP", "none")
        whichWidget = config.getInt("widget", 0)
        when (whichWidget) {
            0 -> remoteView = RemoteViews(applicationContext.packageName, R.layout.widget)
        }
        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(applicationContext)

        // prev button pressed
        if (command == PREV) {
            android.util.Log.i("WIDGET SERVICE", "PREV")
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Left")
            // play button pressed
        } else if (command == PLAY) {
            android.util.Log.i("WIDGET SERVICE", "PLAY")
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Top")
            // next button pressed
        } else if (command == NEXT) {
            android.util.Log.i("WIDGET SERVICE", "NEXT")
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Right")
            // pause widget
        } else if (command == PAUSE) {
            if (isPaused) {
                Widget.Companion.setAlarm(applicationContext, appWidgetId, 1000)
                remoteView.setInt(R.id.pause_button, "setBackgroundResource", R.drawable.pause_on)
                isPaused = false
            } else {
                Widget.Companion.setAlarm(applicationContext, appWidgetId, -1)
                remoteView.setInt(R.id.pause_button, "setBackgroundResource", R.drawable.pause_off)
                isPaused = true
            }
            // update
        } else if (command == UPDATE) {
            android.util.Log.i("WIDGET SERVICE", "UPDATE")
            if (config.getString("activeEmpegIP", "none") == "none") {
                if (!runOnce) {
                    val noIP = android.content.Intent(
                        this,
                        AddEmpeg::class.java
                    )
                    noIP.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(noIP)
                    runOnce = true
                }
            } else {
                playerIP = config.getString("activeEmpegIP", "none")
                android.util.Log.i("WIDGET SERVICE", "($empegCounter, $lastCounter, $thirdCounter)")
                DownloadDataTask().execute("http://$playerIP/proc/empeg_notify")
            }
        }

        when (whichWidget) {
            0 -> {
                // put the text into the textView
                remoteView.setTextViewText(R.id.top_line, topDisplay)
                remoteView.setTextViewText(R.id.bottom_line, bottomDisplay)
                // set buttons
                remoteView.setOnClickPendingIntent(
                    R.id.pause_button, Widget.Companion.makeControlPendingIntent(
                        applicationContext, PAUSE, appWidgetId
                    )
                )
                remoteView.setOnClickPendingIntent(
                    R.id.prev_button, Widget.Companion.makeControlPendingIntent(
                        applicationContext, PREV, appWidgetId
                    )
                )
                remoteView.setOnClickPendingIntent(
                    R.id.play_button, Widget.Companion.makeControlPendingIntent(
                        applicationContext, PLAY, appWidgetId
                    )
                )
                remoteView.setOnClickPendingIntent(
                    R.id.next_button, Widget.Companion.makeControlPendingIntent(
                        applicationContext, NEXT, appWidgetId
                    )
                )
            }
        }
        // apply changes to widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteView)
        super.onStart(intent, startId)
    }

    override fun onBind(arg0: android.content.Intent): IBinder? {
        return null
    }

    inner class DownloadDataTask : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg url: String): String {
            var responseBody = ""
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                android.util.Log.i("EMPEG", "FETCHING: " + url[0])
                val httpget: HttpGet = HttpGet(url[0])
                val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                responseBody = httpclient.execute(httpget, responseHandler)

                httpclient.getConnectionManager().shutdown()
            } catch (e: java.net.MalformedURLException) {
                android.util.Log.i("EMPEG", "MalformedURLException")
            } catch (e: java.io.IOException) {
                android.util.Log.i("EMPEG", "IOException")
            }
            return responseBody
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: String) {
            //			Log.i("EMPEG","onPostExecute: "+result);

            // parse the result

            var result = result
            result = result.replace("(\\r|\\n)".toRegex(), "")
            //			Log.i("","result = "+result);
            val VERSE_PATTERN =
                java.util.regex.Pattern.compile("notify_FidTime = \"(.*?)\";notify_Artist = \"(.*?)\";notify_FID = \"(.*?)\";notify_Genre = \"(.*?)\";notify_MixerInput = \"(.*?)\";notify_Track = \"(.*?)\";notify_Sound = \"(.*?)\";notify_Title = \"(.*?)\";notify_Volume = \"(.*?)\";")
            val m = VERSE_PATTERN.matcher(result)
            while (m.find()) {
                topDisplay = m.group(8)
                bottomDisplay = m.group(2) + " - " + m.group(1).split("  ".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                empegCounter = m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1]
                android.util.Log.i("WIDGET_SERVICE", "empegCounter = $empegCounter")
            }
        }
    }

    inner class sendButton : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg url: String): String {
            var responseBody = ""
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                //				Log.i("EMPEG","FETCHING: "+url[0]);
                val httpget: HttpGet = HttpGet(url[0])
                val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                responseBody = httpclient.execute(httpget, responseHandler)

                httpclient.getConnectionManager().shutdown()
            } catch (e: java.net.MalformedURLException) {
                android.util.Log.i("EMPEG", "MalformedURLException")
            } catch (e: java.io.IOException) {
                android.util.Log.i("EMPEG", "IOException")
            }
            return responseBody
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: String) {
            //			Log.i("EMPEG","onPostExecute: "+result);
        }
    }

    companion object {
        const val UPDATE: String = "update"
        const val PAUSE: String = "pause"
        const val PREV: String = "prev"
        const val PLAY: String = "play"
        const val NEXT: String = "next"
    }
}
