package com.chasinglemons.empeg

import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.inputmethodservice.Keyboard
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Display
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import org.apache.http.HttpResponse
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException

class TabletMain : ListActivity(), OnSharedPreferenceChangeListener, OnLongClickListener,
    OnPanelListener {
    var playerIP: String? = null
    var config: SharedPreferences? = null
    var doVibrate: Boolean = false
    var vibradora: Vibrator? = null
    var screenHandler: Handler? = null
    var errorHandler: Handler? = null
    var sr: Runnable? = null
    var imageURL: String? = null
    var enqueueURL: String? = null
    var appendURL: String? = null
    var insertURL: String? = null
    var streamURL: String? = null
    var screenRefreshRate: Int = 1000
    var doScreenUpdate: Boolean = true
    private var prefListener: OnSharedPreferenceChangeListener? = null
    var mQuickAction: QuickAction? = null

    // Playlist Explorer
    var progresso: ProgressBar? = null
    var gData: GlobalData? = null
    var pHistoryButton: Button? = null
    var pHomeButton: Button? = null
    var pHistoryLayout: LinearLayout? = null
    var pScroller: LinearLayout? = null
    var pNotFound: LinearLayout? = null
    var lview: ListView? = null

    // Remote
    var dataURL: String? = null
    var empegScreen: ImageView? = null
    var empegLens: ImageView? = null
    var vfd: FrameLayout? = null
    var rowG: LinearLayout? = null
    var mWidth: Int = 0
    var mAdjustedWidth: Int = 0
    var mAdjustedHeight: Int = 0
    var activeLens: Int = 0
    var redMatrix: FloatArray =
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f)
    var greenMatrix: FloatArray =
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f)
    var blueMatrix: FloatArray = floatArrayOf(
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0.4f,
        0.35f,
        0f,
        0f,
        0f,
        0f,
        1f,
        0f,
        0f,
        0f,
        0.1f,
        1f,
        0f,
        0f
    )
    var yellowMatrix: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,  //red
        0f, 1f, 0f, 0f, 0f,  //green
        0f, 0f, 0f, 0f, 0f,  //blue
        0.5f, 0.5f, 0f, 0f, 0f //alpha
    )
    var mKeyboard: Keyboard? = null
    var mKeyboardView: CustomKeyboardView? = null
    var kpanel: KeyboardPanel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tablet_main)

        gData = (getApplicationContext() as GlobalData?)

        // Implement Quick Action here.
        // A pop up will have 3  actions for user select:
        val enqueueAction = ActionItem()
        enqueueAction.title = "Enqueue"
        val appendAction = ActionItem()
        appendAction.title = "Append"
        val insertAction = ActionItem()
        insertAction.title = "Insert"
        val streamAction = ActionItem()
        streamAction.title = "Stream"
        mQuickAction = QuickAction(this)
        mQuickAction!!.addActionItem(enqueueAction)
        mQuickAction!!.addActionItem(appendAction)
        mQuickAction!!.addActionItem(insertAction)
        mQuickAction!!.addActionItem(streamAction)

        // setup the action item click listener
        mQuickAction!!.setOnActionItemClickListener(object : OnActionItemClickListener {
            override fun onItemClick(pos: Int) {
                if (pos == 0) { // enqueue selected
                    sendCommand().execute("http://$playerIP$enqueueURL")
                } else if (pos == 1) { // append selected
                    sendCommand().execute("http://$playerIP$appendURL")
                } else if (pos == 2) { // insert selected
                    sendCommand().execute("http://$playerIP$insertURL")
                } else if (pos == 3) { // stream selected
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.parse("http://$playerIP$streamURL"), "audio/*")
                    startActivity(intent)
                }
                if (doVibrate) {
                    vibradora.vibrate(50)
                }
            }
        })

        // References
        vfd = findViewById<View>(R.id.vfdFrame) as FrameLayout?
        rowG = findViewById<View>(R.id.row_g) as LinearLayout?
        pNotFound = findViewById<View>(R.id.List_Not_Found) as LinearLayout?
        pScroller = findViewById<View>(R.id.List_Scroller) as LinearLayout?
        pHistoryLayout = findViewById<View>(R.id.List_Back) as LinearLayout?

        config = PreferenceManager.getDefaultSharedPreferences(this)

        mKeyboard = Keyboard(this, R.xml.keyboard)
        mKeyboardView = findViewById<View>(R.id.keyboard_view) as CustomKeyboardView?
        mKeyboardView!!.keyboard = mKeyboard
        mKeyboardView!!.setOnKeyboardActionListener(BasicOnKeyboardActionListener(this))

        kpanel = findViewById<View>(R.id.kbdPanel) as KeyboardPanel?
        kpanel!!.setOnPanelListener(this)
        kpanel!!.setInterpolator(ExpoInterpolator(EasingType.Type.OUT))
        if (config.getBoolean("showKeyboard", true) == false) {
            kpanel!!.visibility = View.GONE
        }

        vibradora = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        doVibrate = config.getBoolean("doVibrate", true)
        doScreenUpdate = config.getBoolean("doScreenUpdate", true)
        screenRefreshRate = config.getString("refreshRate", "1000").toInt()

        if (config.getBoolean("showScreen", true) == false) {
            vfd.setVisibility(View.GONE)
        }
        if (config.getBoolean("showHijack", false) == true) {
            rowG.setVisibility(View.VISIBLE)
        }

        if (config.getString("activeEmpegIP", "none") == "none") {
            startActivityForResult(Intent(this, AddEmpeg::class.java), ADD_IP_REQUEST_CODE)
        } else {
            playerIP = config.getString("activeEmpegIP", "none")
            DownloadDataTask().execute("http://$playerIP/?FID=101&EXT=.htm", "add")
            if (config.getBoolean("doNotifications", true)) {
                val service: Intent = Intent(
                    this,
                    NotificationService::class.java
                )
                this.startService(service)
            }
        }

        val display: Display = getWindowManager().getDefaultDisplay()
        val size = Point()
        display.getSize(size)
        mWidth = size.x
        mAdjustedWidth = (mWidth * .666f).toInt()
        mAdjustedHeight = (mAdjustedWidth * .2f).toInt()

        val initScreen = Bitmap.createScaledBitmap(
            BitmapFactory.decodeStream(
                this.getResources()
                    .openRawResource(
                        getResources().getIdentifier(
                            "com.chasinglemons.empeg:drawable/empeg_screen_init",
                            null, null
                        )
                    )
            ), mAdjustedWidth, mAdjustedHeight, false
        )

        empegScreen = findViewById<View>(R.id.blitScreen) as ImageView?
        empegScreen!!.layoutParams.height = mAdjustedHeight
        empegScreen.setColorFilter(colorFilter)
        empegScreen!!.setImageBitmap(initScreen)

        val a1 = findViewById<View>(R.id.remote_a1) as ImageView
        a1.setOnLongClickListener(this)
        val a2 = findViewById<View>(R.id.remote_a2) as ImageView
        a2.setOnLongClickListener(this)
        val a3 = findViewById<View>(R.id.remote_a3) as ImageView
        a3.setOnLongClickListener(this)
        val a4 = findViewById<View>(R.id.remote_a4) as ImageView
        a4.setOnLongClickListener(this)
        val b1 = findViewById<View>(R.id.remote_b1) as ImageView
        b1.setOnLongClickListener(this)
        val b2 = findViewById<View>(R.id.remote_b2) as ImageView
        b2.setOnLongClickListener(this)
        val b3 = findViewById<View>(R.id.remote_b3) as ImageView
        b3.setOnLongClickListener(this)
        val b4 = findViewById<View>(R.id.remote_b4) as ImageView
        b4.setOnLongClickListener(this)
        val c1 = findViewById<View>(R.id.remote_c1) as ImageView
        c1.setOnLongClickListener(this)
        val c2 = findViewById<View>(R.id.remote_c2) as ImageView
        c2.setOnLongClickListener(this)
        val c3 = findViewById<View>(R.id.remote_c3) as ImageView
        c3.setOnLongClickListener(this)
        val c4 = findViewById<View>(R.id.remote_c4) as ImageView
        c4.setOnLongClickListener(this)
        val d1 = findViewById<View>(R.id.remote_d1) as ImageView
        d1.setOnLongClickListener(this)
        val d2 = findViewById<View>(R.id.remote_d2) as ImageView
        d2.setOnLongClickListener(this)
        val d3 = findViewById<View>(R.id.remote_d3) as ImageView
        d3.setOnLongClickListener(this)
        val d4 = findViewById<View>(R.id.remote_d4) as ImageView
        d4.setOnLongClickListener(this)
        val e1 = findViewById<View>(R.id.remote_e1) as ImageView
        e1.setOnLongClickListener(this)
        val e2 = findViewById<View>(R.id.remote_e2) as ImageView
        e2.setOnLongClickListener(this)
        val e3 = findViewById<View>(R.id.remote_e3) as ImageView
        e3.setOnLongClickListener(this)
        val e4 = findViewById<View>(R.id.remote_e4) as ImageView
        e4.setOnLongClickListener(this)
        val f1 = findViewById<View>(R.id.remote_f1) as ImageView
        f1.setOnLongClickListener(this)
        val f2 = findViewById<View>(R.id.remote_f2) as ImageView
        f2.setOnLongClickListener(this)
        val f3 = findViewById<View>(R.id.remote_f3) as ImageView
        f3.setOnLongClickListener(this)
        val f4 = findViewById<View>(R.id.remote_f4) as ImageView
        f4.setOnLongClickListener(this)

        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        prefListener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
                doScreenUpdate = config.getBoolean("doScreenUpdate", true)
                if (key == "doScreenUpdate") {
                    if (!doScreenUpdate) {
                        val initScreen = Bitmap.createScaledBitmap(
                            BitmapFactory.decodeStream(
                                getResources()
                                    .openRawResource(
                                        getResources().getIdentifier(
                                            "com.chasinglemons.empeg:drawable/empeg_screen_init",
                                            null, null
                                        )
                                    )
                            ), mAdjustedWidth, mAdjustedHeight, false
                        )
                        empegScreen!!.setImageBitmap(initScreen)
                    }
                }
                if (key == "refreshRate") {
                    screenRefreshRate = config.getString("refreshRate", "1000").toInt()
                }
                if (key == "pixelFont") {
                    getListView().invalidateViews()
                }
                if (key == "activeLens") {
                    empegScreen.setColorFilter(this.colorFilter)
                    lview = getListView()
                    lview!!.divider = GradientDrawable(
                        GradientDrawable.Orientation.RIGHT_LEFT,
                        this.colorScheme
                    )
                    lview!!.dividerHeight = 1
                }
                if (config.getBoolean("showScreen", true) == false) {
                    vfd.setVisibility(View.GONE)
                } else {
                    vfd.setVisibility(View.VISIBLE)
                }
                if (config.getBoolean("showHijack", false) == false) {
                    rowG.setVisibility(View.GONE)
                } else {
                    rowG.setVisibility(View.VISIBLE)
                }
                if (config.getBoolean("showKeyboard", false) == false) {
                    kpanel!!.visibility = View.GONE
                } else {
                    kpanel!!.visibility = View.VISIBLE
                }
                if (config.getBoolean("doNotifications", true) == false) {
                    val service = Intent(
                        getApplicationContext(),
                        NotificationService::class.java
                    )
                    stopService(service)
                } else {
                    val service = Intent(
                        getApplicationContext(),
                        NotificationService::class.java
                    )
                    startService(service)
                }
            }
        }
        config.registerOnSharedPreferenceChangeListener(prefListener)

        // Set up the action bar.
        val actionBar: ActionBar = getActionBar()
        /*		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);*/
        actionBar.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        val retryComm = findViewById<View>(R.id.button_refresher) as Button
        retryComm.setOnClickListener {
            pScroller.setVisibility(View.VISIBLE)
            pNotFound.setVisibility(View.GONE)
            DownloadDataTask()
                .execute("http://$playerIP/?FID=101&EXT=.htm", "add")
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }

        pHistoryButton = findViewById<View>(R.id.button_pl_up) as Button?
        pHistoryButton!!.setOnClickListener {
            gData!!.playlistHistory.removeAt(gData!!.playlistHistory.size - 1)
            DownloadDataTask().execute(
                "http://$playerIP" + gData!!.playlistHistory[gData!!.playlistHistory.size - 1] + "&EXT=.htm",
                "noAdd"
            )
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }

        pHomeButton = findViewById<View>(R.id.button_pl_home) as Button?
        pHomeButton!!.setOnClickListener {
            gData!!.playlistHistory.clear()
            DownloadDataTask()
                .execute("http://$playerIP/?FID=101&EXT=.htm", "add")
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //Log.i("TABLETMAIN","onResume()");
        playerIP = config.getString("activeEmpegIP", "none")

        //Log.i("TABLETMAIN","saved IP: "+playerIP);
        if (playerIP != "none") {
            imageURL = "http://$playerIP/proc/empeg_screen.png"
            if (config.getBoolean("doScreenUpdate", true) == true && config.getBoolean(
                    "showScreen",
                    true
                ) == true
            ) {
                refreshScreen()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Log.i("TABLETMAIN","onPause()");
        if (screenHandler != null) {
            screenHandler!!.removeCallbacks(sr!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val service: Intent = Intent(
            this,
            NotificationService::class.java
        )
        stopService(service)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        //Log.i("ONACTIVITYRESULT","requestCode="+requestCode+", resultCode="+resultCode+", data="+data);
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ADD_IP_REQUEST_CODE -> if (config.getString("activeEmpegIP", "none") != "none") {
                if (screenHandler != null) {
                    screenHandler!!.removeCallbacks(sr!!)
                }
                playerIP = config.getString("activeEmpegIP", "none")

                imageURL = "http://$playerIP/proc/empeg_screen.png"

                // send a hello msg to the empeg
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=NODATA&POPUP%201%20%20Empeg%20Remote%20configured")

                // load/reload the playlist
                DownloadDataTask().execute("http://$playerIP/?FID=101&EXT=.htm", "add")

                refreshScreen()

                if (config.getBoolean("doNotifications", true)) {
                    val service: Intent = Intent(
                        this,
                        NotificationService::class.java
                    )
                    this.startService(service)
                }
            } else {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_discovery) {
            startActivityForResult(Intent(this, AddEmpeg::class.java), ADD_IP_REQUEST_CODE)
            return true
        } else if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, Settings::class.java))
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key != null) {
            //Log.i("PAGER_ACTIVITY","onSharedPreferenceChanged: key: "+key);
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
                //Log.i("TABLETMAIN","MalformedURLException - sendCommand");
            } catch (e: IOException) {
                //Log.i("TABLETMAIN","IOException - sendCommand");
            }
            return responseBody
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: String) {
            //			//Log.i("EMPEG","onPostExecute: "+result);
        }
    }

    fun buttonPress(v: View) {
        if (v.id == R.id.remote_a1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=One")
        } else if (v.id == R.id.remote_a2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Two")
        } else if (v.id == R.id.remote_a3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Three")
        } else if (v.id == R.id.remote_a4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Source")
        } else if (v.id == R.id.remote_b1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Four")
        } else if (v.id == R.id.remote_b2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Five")
        } else if (v.id == R.id.remote_b3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Six")
        } else if (v.id == R.id.remote_b4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Tuner")
        } else if (v.id == R.id.remote_c1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Seven")
        } else if (v.id == R.id.remote_c2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Eight")
        } else if (v.id == R.id.remote_c3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Nine")
        } else if (v.id == R.id.remote_c4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=SelectMode")
        } else if (v.id == R.id.remote_d1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Cancel")
        } else if (v.id == R.id.remote_d2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Zero")
        } else if (v.id == R.id.remote_d3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Search")
        } else if (v.id == R.id.remote_d4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Sound")
        } else if (v.id == R.id.remote_e1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Prev")
        } else if (v.id == R.id.remote_e2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Next")
        } else if (v.id == R.id.remote_e3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Menu")
        } else if (v.id == R.id.remote_e4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=KnobRight")
        } else if (v.id == R.id.remote_f1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Info")
        } else if (v.id == R.id.remote_f2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Visual")
        } else if (v.id == R.id.remote_f3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Play")
        } else if (v.id == R.id.remote_f4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=KnobLeft")
        } else if (v.id == R.id.remote_g1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=HijackMenu")
        }
        if (doVibrate) {
            vibradora.vibrate(50)
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.remote_a1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=One.L")
        } else if (v.id == R.id.remote_a2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Two.L")
        } else if (v.id == R.id.remote_a3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Three.L")
        } else if (v.id == R.id.remote_a4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Source.L")
        } else if (v.id == R.id.remote_b1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Four.L")
        } else if (v.id == R.id.remote_b2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Five.L")
        } else if (v.id == R.id.remote_b3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Six.L")
        } else if (v.id == R.id.remote_b4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Tuner.L")
        } else if (v.id == R.id.remote_c1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Seven.L")
        } else if (v.id == R.id.remote_c2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Eight.L")
        } else if (v.id == R.id.remote_c3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Nine.L")
        } else if (v.id == R.id.remote_c4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=SelectMode.L")
        } else if (v.id == R.id.remote_d1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Cancel.L")
        } else if (v.id == R.id.remote_d2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Zero.L")
        } else if (v.id == R.id.remote_d3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Search.L")
        } else if (v.id == R.id.remote_d4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Sound.L")
        } else if (v.id == R.id.remote_e1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Prev.L")
        } else if (v.id == R.id.remote_e2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Next.L")
        } else if (v.id == R.id.remote_e3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Menu.L")
        } else if (v.id == R.id.remote_e4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=VolUp.L")
        } else if (v.id == R.id.remote_f1) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Info.L")
        } else if (v.id == R.id.remote_f2) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Visual.L")
        } else if (v.id == R.id.remote_f3) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Play.L")
        } else if (v.id == R.id.remote_f4) {
            sendCommand().execute("http://$playerIP/proc/empeg_notify?button=VolDown.L")
        }
        if (doVibrate) {
            vibradora.vibrate(50)
        }
        return true
    }

    private val colorFilter: ColorFilter?
        get() {
            activeLens = config.getString("activeLens", "0").toInt()
            return when (activeLens) {
                0 -> ColorMatrixColorFilter(blueMatrix)
                1 -> ColorMatrixColorFilter(redMatrix)
                2 -> ColorMatrixColorFilter(yellowMatrix)
                3 -> ColorMatrixColorFilter(greenMatrix)
                4 -> null // white = no matrix involvement
                else -> ColorMatrixColorFilter(blueMatrix)
            }
        }

    private val colorScheme: IntArray
        get() {
            activeLens = config.getString("activeLens", "0").toInt()
            return when (activeLens) {
                0 -> intArrayOf(0, -0xcc4a1b, 0)
                1 -> intArrayOf(0, -0x10000, 0)
                2 -> intArrayOf(0, -0x100, 0)
                3 -> intArrayOf(0, -0xff0100, 0)
                4 -> intArrayOf(0, -0x1, 0)
                else -> intArrayOf(0, -0xcc4a1b, 0)
            }
        }

    private val progressBar: Int
        get() {
            activeLens = config.getString("activeLens", "0").toInt()
            return when (activeLens) {
                0 -> R.id.blue_progressbar
                1 -> R.id.red_progressbar
                2 -> R.id.yellow_progressbar
                3 -> R.id.green_progressbar
                4 -> R.id.white_progressbar
                else -> R.id.blue_progressbar
            }
        }

    fun playHandler(v: View) {
        //Log.i("TABLETMAIN","playhandler() pressed...");
        //get the row the clicked button is in
        val vwParentRow: LinearLayout = v.parent as LinearLayout
        val btnChild = vwParentRow.getChildAt(0) as ImageView
        //Log.i("TABLETMAIN","playhandler "+btnChild.getTag());
        sendCommand().execute("http://" + playerIP + btnChild.tag)
        if (doVibrate) {
            vibradora.vibrate(50)
        }
    }

    fun plEditHandler(v: View) {
        //get the row the clicked button is in
        val vwParentRow: LinearLayout = v.parent as LinearLayout
        val btnChild = vwParentRow.getChildAt(2) as ImageView

        //Log.i("TABLETMAIN","plEdithandler "+btnChild.getTag());

        // explode tag
        val tagURLs = btnChild.tag.toString().split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        enqueueURL = tagURLs[0]
        appendURL = tagURLs[1]
        insertURL = tagURLs[2]
        streamURL = tagURLs[3]

        // show quickaction now that the URLs are populated
        mQuickAction!!.show(v)

        if (doVibrate) {
            vibradora.vibrate(50)
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            if (e.keyCode == KeyEvent.KEYCODE_A || e.keyCode == KeyEvent.KEYCODE_B || e.keyCode == KeyEvent.KEYCODE_C || e.keyCode == KeyEvent.KEYCODE_2) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Two")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_D || e.keyCode == KeyEvent.KEYCODE_E || e.keyCode == KeyEvent.KEYCODE_F || e.keyCode == KeyEvent.KEYCODE_3) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Three")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_G || e.keyCode == KeyEvent.KEYCODE_H || e.keyCode == KeyEvent.KEYCODE_I || e.keyCode == KeyEvent.KEYCODE_4) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Four")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_J || e.keyCode == KeyEvent.KEYCODE_K || e.keyCode == KeyEvent.KEYCODE_L || e.keyCode == KeyEvent.KEYCODE_5) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Five")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_M || e.keyCode == KeyEvent.KEYCODE_N || e.keyCode == KeyEvent.KEYCODE_O || e.keyCode == KeyEvent.KEYCODE_6) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Six")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_P || e.keyCode == KeyEvent.KEYCODE_R || e.keyCode == KeyEvent.KEYCODE_S || e.keyCode == KeyEvent.KEYCODE_7) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Seven")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_T || e.keyCode == KeyEvent.KEYCODE_U || e.keyCode == KeyEvent.KEYCODE_V || e.keyCode == KeyEvent.KEYCODE_8) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Eight")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_W || e.keyCode == KeyEvent.KEYCODE_X || e.keyCode == KeyEvent.KEYCODE_Y || e.keyCode == KeyEvent.KEYCODE_9) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Nine")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_Q || e.keyCode == KeyEvent.KEYCODE_Z || e.keyCode == KeyEvent.KEYCODE_0) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Zero")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_ENTER) {
                KeyboardPanel.Companion.setClosed()
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Menu")
                return true
            }
            if (e.keyCode == KeyEvent.KEYCODE_DEL) {
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=Cancel")
                return true
            }
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }
        return super.dispatchKeyEvent(e)
    }

    private fun refreshScreen() {
        screenHandler = Handler()
        sr = Runnable {
            if (doScreenUpdate) {
                //					//Log.i("","trying "+imageURL);
                DownloadImageTask().execute(imageURL)
                screenHandler!!.postDelayed(sr!!, screenRefreshRate.toLong())
            }
        }
        screenHandler!!.post(sr)
    }

    private inner class DownloadImageTask : AsyncTask<String?, String?, Bitmap?>() {
        override fun doInBackground(vararg urls: String): Bitmap? {
            var updatedScreen: Bitmap? = null
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                val httpget: HttpGet = HttpGet(urls[0])
                val response: HttpResponse = httpclient.execute(httpget)
                val `in`: InputStream = response.getEntity().getContent()
                val bis = BufferedInputStream(`in`, 8192)
                updatedScreen = BitmapFactory.decodeStream(bis)
                httpclient.getConnectionManager().shutdown()
            } catch (e: MalformedURLException) {
                //Log.i("TABLETMAIN","MalformedURLException - DownloadImageTask");
            } catch (e: IOException) {
                //Log.i("TABLETMAIN","IOException - DownloadImageTask");
                show404()
            }
            return updatedScreen
        }

        override fun onPostExecute(result: Bitmap?) {
            var updatedScreen: Bitmap? = null
            updatedScreen = if (result != null) {
                Bitmap.createScaledBitmap(
                    result,
                    mAdjustedWidth,
                    mAdjustedHeight,
                    false
                )
            } else {
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeStream(
                        getResources()
                            .openRawResource(
                                getResources().getIdentifier(
                                    "com.chasinglemons.empeg:drawable/not_found",
                                    null,
                                    null
                                )
                            )
                    ),
                    mAdjustedWidth, mAdjustedHeight, false
                )
            }
            empegScreen!!.setImageBitmap(updatedScreen)
        }
    }

    inner class DownloadDataTask :
        AsyncTask<String?, String?, ArrayList<String?>?>() {
        var result: ArrayList<String> = ArrayList()

        override fun onPreExecute() {
            progresso = findViewById<View>(this.progressBar) as ProgressBar?
            progresso!!.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg url: String): ArrayList<String> {
            // progressor.setVisibility(View.VISIBLE);

            try {
                val httpclient: HttpClient = DefaultHttpClient()
                // //Log.i("EMPEG","FETCHING: "+url[0]);
                val httpget: HttpGet = HttpGet(url[0])
                val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                result.add(httpclient.execute(httpget, responseHandler))

                httpclient.getConnectionManager().shutdown()
            } catch (e: MalformedURLException) {
                //Log.i("PLAYLISTEXPLORER","DownloadDataTask MalformedURLException: "+e);
            } catch (e: IOException) {
                //Log.i("PLAYLISTEXPLORER","DownloadDataTask IOException: "+e);
                // empeg not found
                show404()
            }

            result.add(url[1]) //to add to history or to not add to history...

            return result
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: ArrayList<String?>) {
            //Log.i("EMPEG","onPostExecute result.get(0): "+result.get(0));
            if (result[0] != null) {
                // parse the result
                val pList: MutableList<Playlist> = ArrayList()
                val pListElements: MutableList<String> = ArrayList()
                val pListLinks: MutableList<String> = ArrayList()
                var pName = ""
                var pLength = ""
                var pType = ""
                var pArtist = ""
                var pSource = ""
                val doc = Jsoup.parse(result[0])
                val trs = doc.getElementsByTag("tr")
                for (tr in trs) {
                    val tds = tr.getElementsByTag("td")

                    for (td in tds) {
                        // //Log.i("EMPEG_TD","sibindex: "+td.elementSiblingIndex());
                        /*if (td.elementSiblingIndex() > 5) {
						pList.add(td.text());
					} else {
						pList.add(td.attr("href"));
					}*/
                        // //Log.i("EMPEG_TD",""+td.html());

                        val link = td.select("a").first()
                        try {
                            // //Log.i("EMPEG_TD","linkHref: "+link.attr("href"));
                            pListLinks.add(link!!.attr("href"))
                        } catch (e: NullPointerException) {
                        }

                        if (td.elementSiblingIndex() == 5) {
                            // //Log.i("EMPEG_TD","NAME: "+td.text());
                            pName = td.text()
                        }
                        if (td.elementSiblingIndex() == 6) {
                            // //Log.i("EMPEG_TD","LENGTH: "+td.text());
                            pLength = td.text()
                        }
                        if (td.elementSiblingIndex() == 7) {
                            // //Log.i("EMPEG_TD","TYPE: "+td.text());
                            pType = td.text()
                        }
                        if (td.elementSiblingIndex() == 8) {
                            // //Log.i("EMPEG_TD","ARTIST: "+td.text());
                            pArtist = td.text()
                        }
                        if (td.elementSiblingIndex() == 9) {
                            // //Log.i("EMPEG_TD","SOURCE: "+td.text());
                            pSource = td.text()
                        }
                    }

                    // build the INSERT link
                    val myInsert = pListLinks[2].replace("-", "!")

                    // String pName, String pStreamURL, String pPlayURL, String pInsertURL, String pEnqueueURL, String pAppendURL, String pURL, String pLength, String pType, String pArtist, String pSource
                    if (pListLinks.size > 5) {
                        //Log.e("EMPEG_TD","pLiskLinks "+pListLinks.toString());
                        //					//Log.e("EMPEG_TD","pLiskLinks.get(5) "+pListLinks.get(5));
                        if (pListLinks[5].endsWith(".mp3")) { // this is a song, not a dir
                            //						//Log.e("EMPEG_TD","I am the end of the list!!!!");
                            pList.add(
                                Playlist(
                                    pName,
                                    pListLinks[0],
                                    pListLinks[1], myInsert,
                                    pListLinks[2],
                                    pListLinks[3], "none", pLength, pType, pArtist, pSource
                                )
                            )
                        } else {
                            pList.add(
                                Playlist(
                                    pName,
                                    pListLinks[0],
                                    pListLinks[1], myInsert,
                                    pListLinks[2],
                                    pListLinks[3], pListLinks[5], pLength, pType, pArtist, pSource
                                )
                            )
                        }
                    } else {
                        // //Log.e("EMPEG_TD","HEADLIST");
                        pList.add(
                            Playlist(
                                "000000$pName",
                                pListLinks[0],
                                pListLinks[1],
                                myInsert,
                                pListLinks[2],
                                pListLinks[4],
                                "head",
                                pLength,
                                pType,
                                pArtist,
                                pSource
                            )
                        )

                        // //Log.i("PLAYLIST_EXPLORER","pListLinks.get(4) = "+pListLinks.get(4));
                        if (result[1] == "add") {
                            gData!!.playlistHistory.add(pListLinks[4]) // write this URL to playlist history
                        }
                        if (pName != "All Music" && pListLinks.size > 1 /* if nothing is returned (player off or not configured)*/) {
                            pHistoryLayout.setVisibility(View.VISIBLE)
                        } else {
                            pHistoryLayout.setVisibility(View.GONE)
                        }
                    }
                    pListElements.clear()
                    pListLinks.clear()
                }

                // Sort the list
                /*				Collections.sort(pList, new Comparator<Playlist>() {               
					@Override
					public int compare(Playlist p1, Playlist p2) {
						return p1.getpName().compareTo(p2.getpName());
					}
				});*/
                setListAdapter(PlaylistAdapter(this@TabletMain, pList))
                lview = getListView()
                lview!!.divider = GradientDrawable(
                    GradientDrawable.Orientation.RIGHT_LEFT,
                    this.colorScheme
                )
                lview!!.dividerHeight = 1
            }
            progresso!!.visibility = View.GONE
        }
    }

    fun playlistHandler(v: View) {
        //get the row the clicked button is in
        val vwParentRow: LinearLayout = v.parent as LinearLayout
        val btnChild = vwParentRow.getChildAt(0) as Button
        // //Log.i("EMPEG","playlisthandler "+btnChild.getTag());
        DownloadDataTask().execute("http://" + playerIP + btnChild.tag)
        // vwParentRow.refreshDrawableState();       
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        if (position > 0) {
            val item = getListAdapter().getItem(position) as Playlist
            //			 //Log.i("PLAYLISTEXPL","onListItemClick item.getpURL() = "+item.getpURL());
            if (item.getpURL() != "none") {
                DownloadDataTask().execute("http://" + playerIP + item.getpURL(), "add")
                if (doVibrate) {
                    vibradora.vibrate(50)
                }
            }
        }
    }

    private fun show404() {
        errorHandler = Handler(Looper.getMainLooper())
        errorHandler!!.post {
            pScroller.setVisibility(View.GONE)
            pHistoryLayout.setVisibility(View.GONE)
            progresso!!.visibility = View.GONE
            pNotFound.setVisibility(View.VISIBLE)
        }
    }

    override fun onPanelClosed(panel: KeyboardPanel?) {
        // TODO Auto-generated method stub
    }

    override fun onPanelOpened(panel: KeyboardPanel?) {
        // TODO Auto-generated method stub
    }

    companion object {
        protected const val ADD_IP_REQUEST_CODE: Int = 100
        const val MENU_DISCOVERY: Int = Menu.FIRST
        const val MENU_PLAYLISTS: Int = Menu.FIRST + 1
        const val MENU_WIDGETS: Int = Menu.FIRST + 2
        const val MENU_SETTINGS: Int = Menu.FIRST + 3
    }
}
