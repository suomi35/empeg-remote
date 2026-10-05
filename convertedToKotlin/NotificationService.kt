package com.chasinglemons.empeg

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import org.apache.http.client.HttpClient
import java.io.IOException
import java.net.MalformedURLException
import java.util.regex.Pattern

@SuppressLint("NewApi")
class NotificationService : Service() {
    private var mNM: NotificationManager? = null
    private val handler = Handler()
    var config: SharedPreferences? = null
    var playerIP: String? = null

    //	private NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    //Different Id's will show up as different notifications
    //	private int mNotificationId = 1;
    private val firstTime = true
    var appIntent: Intent? = null
    var button1Intent: Intent? = null
    var button2Intent: Intent? = null
    var button3Intent: Intent? = null
    var button4Intent: Intent? = null
    var pIntent: PendingIntent? = null
    var pb1: PendingIntent? = null
    var pb2: PendingIntent? = null
    var pb3: PendingIntent? = null
    var pb4: PendingIntent? = null
    var contentView: RemoteViews? = null
    var notificator: Notification? = null

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    inner class LocalBinder : Binder() {
        val service: NotificationService
            get() = this@NotificationService
    }

    override fun onCreate() {
        appIntent = Intent(this, Start::class.java)
        appIntent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        pIntent = PendingIntent.getActivity(this, 0, appIntent, 0)

        button1Intent = Intent(this, NotifButtonListener::class.java)
        button1Intent!!.putExtra("action", "up")
        pb1 = PendingIntent.getBroadcast(this, 0, button1Intent, 351)

        button2Intent = Intent(this, NotifButtonListener::class.java)
        button2Intent!!.putExtra("action", "left")
        pb2 = PendingIntent.getBroadcast(this, 0, button2Intent, 352)

        button3Intent = Intent(this, NotifButtonListener::class.java)
        button3Intent!!.putExtra("action", "right")
        pb3 = PendingIntent.getBroadcast(this, 0, button3Intent, 353)

        button4Intent = Intent(this, NotifButtonListener::class.java)
        button4Intent!!.putExtra("action", "down")
        pb4 = PendingIntent.getBroadcast(this, 0, button4Intent, 354)

        contentView = RemoteViews(packageName, R.layout.custom_notification)
        contentView.setOnClickPendingIntent(R.id.imageButton1, pb1)
        contentView.setOnClickPendingIntent(R.id.imageButton2, pb2)
        contentView.setOnClickPendingIntent(R.id.imageButton3, pb3)
        contentView.setOnClickPendingIntent(R.id.imageButton4, pb4)

        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        config = PreferenceManager.getDefaultSharedPreferences(this)
        playerIP = config.getString("activeEmpegIP", "none")
        // Display a notification about us starting.  We put an icon in the status bar.
        updateNotification("", "", "")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handler.removeCallbacks(sendUpdatesToNotif)
        handler.postDelayed(sendUpdatesToNotif, 1000) // 1 second
        return START_STICKY
    }

    override fun onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(0)

        handler.removeCallbacks(sendUpdatesToNotif)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private val mBinder: IBinder = LocalBinder()

    /**
     * Show a notification while this service is running.
     * @return
     */
    @SuppressLint("NewApi")
    private fun updateNotification(eArtist: String, eTitle: String, eTime: String) {
        contentView.setTextViewText(R.id.notification_title, "Empeg Remote")
        contentView.setTextViewText(R.id.notification_text, "$eArtist - $eTitle $eTime")

        if (firstTime) {
            notificator = Notification.Builder(this)
                .setContentTitle("Empeg Remote")
                .setContentText("$eArtist - $eTitle $eTime")
                .setContentText("")
                .setSmallIcon(R.drawable.player)
                .setOnlyAlertOnce(true)
                .setContentIntent(pIntent)
                .setWhen(0)
                .build()

            /*			mBuilder.setSmallIcon(R.drawable.ic_launcher)
			.setStyle(new NotificationCompat.InboxStyle())
			.setOnlyAlertOnce(true)
			.setContentIntent(pIntent)
			.setWhen(0)
			.setContent(contentView);
			firstTime = false;*/
        }


//		notificator.setLatestEventInfo(this, "Empeg Remote", eArtist+" - "+eTitle+" "+eTime, pIntent);
        notificator!!.bigContentView = contentView

        mNM.notify(0, notificator)

        //		mBuilder.setContentText(eArtist+" - "+eTitle+" "+eTime);

        //		mNM.notify(mNotificationId, mBuilder.build());
    }

    /*    private void showNotification() {

    	Intent intent = new Intent(this, Start.class);
    	PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

    	Intent switchIntent = new Intent(this, TestButtonListener.class);
        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, switchIntent, 0);

    	// Build notification
    	noti = new NotificationCompat.Builder(this)
    	        .setContentTitle("Empeg Remote")
    	        .setOnlyAlertOnce(true)
    	        .setContentText("Artist - Song - (time)")
    	        .setSmallIcon(R.drawable.ic_launcher)
    	        .setContentIntent(pIntent)
    	        .addAction(R.drawable.button_a4sm, "Power", pendingSwitchIntent)
    	        .addAction(R.drawable.button_f3sm, "Play/Pause", pIntent).build();

        // Send the notification.
        mNM.notify(NOTIFICATION, noti);
    }*/
    private val sendUpdatesToNotif: Runnable = object : Runnable {
        override fun run() {
            DownloadDataTask().execute("http://$playerIP/proc/empeg_notify")

            handler.postDelayed(this, 1000) // 1 second
        }
    }

    inner class DownloadDataTask : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg url: String): String {
            var responseBody = ""
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                //				Log.i("EMPEG","FETCHING: "+url[0]);
                val httpget: HttpGet = HttpGet(url[0])
                val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                responseBody = httpclient.execute(httpget, responseHandler)

                httpclient.getConnectionManager().shutdown()
            } catch (e: MalformedURLException) {
//				Log.i("EMPEG","MalformedURLException");
            } catch (e: IOException) {
//				Log.i("EMPEG","IOException");
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
                Pattern.compile("notify_FidTime = \"(.*?)\";notify_Artist = \"(.*?)\";notify_FID = \"(.*?)\";notify_Genre = \"(.*?)\";notify_MixerInput = \"(.*?)\";notify_Track = \"(.*?)\";notify_Sound = \"(.*?)\";notify_Title = \"(.*?)\";notify_Volume = \"(.*?)\";")
            val m = VERSE_PATTERN.matcher(result)
            while (m.find()) {
                val timeHolder = if (m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].startsWith("0:")) {
                    if (m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].startsWith("0:0")) {
                        m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].substring(3)
                    } else {
                        m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].substring(2)
                    }
                } else {
                    m.group(1).split("  ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                }

                updateNotification(m.group(2), m.group(8), "($timeHolder)")
            }
        }
    }

    class NotifButtonListener : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val config: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val playerIP: String = config.getString("activeEmpegIP", "none")

            val extras = intent.extras
            if (extras != null) {
                if (extras.getString("action") == "up") {
                    sendCommand().execute(
                        "http://$playerIP/proc/empeg_notify?button=Top"
                    )
                } else if (extras.getString("action") == "left") {
                    sendCommand().execute(
                        "http://$playerIP/proc/empeg_notify?button=Left"
                    )
                } else if (extras.getString("action") == "right") {
                    sendCommand().execute(
                        "http://$playerIP/proc/empeg_notify?button=Right"
                    )
                } else if (extras.getString("action") == "down") {
                    sendCommand().execute(
                        "http://$playerIP/proc/empeg_notify?button=Bottom"
                    )
                }
            }
        }

        inner class sendCommand : AsyncTask<String?, String?, String?>() {
            override fun doInBackground(vararg url: String): String {
                var responseBody = ""
                try {
                    val httpclient: HttpClient = DefaultHttpClient()
                    //				//Log.i("EMPEG","FETCHING: "+url[0]);
                    val httpget: HttpGet = HttpGet(url[0])
                    val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                    responseBody = httpclient.execute(httpget, responseHandler)

                    httpclient.getConnectionManager().shutdown()
                } catch (e: MalformedURLException) {
                    //Log.i("PHONEMAIN","MalformedURLException - sendCommand");
                } catch (e: IOException) {
                    //Log.i("PHONEMAIN","IOException - sendCommand");
                }
                return responseBody
            }

            override fun onProgressUpdate(vararg progress: String) {
            }

            override fun onPostExecute(result: String) {
                //			//Log.i("EMPEG","onPostExecute: "+result);
            }
        }
    }
}