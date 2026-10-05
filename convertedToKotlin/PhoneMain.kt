package com.chasinglemons.empeg

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.FragmentTransaction
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.view.Display
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import io.ktor.client.HttpClient
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.util.Locale

class PhoneMain : FragmentActivity(), ActionBar.TabListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    var mViewPager: ViewPager? = null
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
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    var mQuickAction: QuickAction? = null
    var mAdjustedWidth: Int = 0
    var mAdjustedHeight: Int = 0
    var cPager: CustomViewPager? = null
    var client: HttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone_main)

        // Implement Quick Action here.
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

        config = PreferenceManager.getDefaultSharedPreferences(this)

        vibradora = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        doVibrate = config.getBoolean("doVibrate", true)
        doScreenUpdate = config.getBoolean("doScreenUpdate", true)
        screenRefreshRate = config.getString("refreshRate", "1000").toInt()

        if (config.getString("activeEmpegIP", "none") == "none") {
            startActivityForResult(Intent(this, AddEmpeg::class.java), ADD_IP_REQUEST_CODE)
        } else {
            playerIP = config.getString("activeEmpegIP", "none")
            if (config.getBoolean("doNotifications", true)) {
                val service: Intent = Intent(
                    this,
                    NotificationService::class.java
                )
                this.startService(service)
            }
        }

        cPager = findViewById<View>(R.id.pager) as CustomViewPager?
        if (config.getString("swipeAction", "0") == "1") { // disable viewpager swiping
            cPager!!.setPagingEnabled(false)
        }

        val display: Display = getWindowManager().getDefaultDisplay()
        val size = Point()
        display.getSize(size)
        mAdjustedWidth = (size.x * .96f).toInt()
        mAdjustedHeight = (mAdjustedWidth * .25f).toInt()

        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        prefListener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
                doScreenUpdate = config.getBoolean("doScreenUpdate", true)
                if (key == "doScreenUpdate") {
                    if (!doScreenUpdate) {
                        val intent = Intent("screen-update")
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
                        intent.putExtra("updatedScreen", initScreen)
                        LocalBroadcastManager.getInstance(this@PhoneMain).sendBroadcast(intent)
                    }
                }
                if (key == "refreshRate") {
                    screenRefreshRate = config.getString("refreshRate", "1000").toInt()
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
                if (key == "swipeAction") {
                    if (config.getString("swipeAction", "0") == "1") {
                        cPager!!.setPagingEnabled(false) // disable viewpager swiping
                    } else {
                        cPager!!.setPagingEnabled(true)
                    }
                }
            }
        }
        config.registerOnSharedPreferenceChangeListener(prefListener)

        // Set up the action bar.
        val actionBar: ActionBar = getActionBar()
        /*		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);*/
        actionBar.navigationMode = ActionBar.NAVIGATION_MODE_TABS

        // Create the adapter that will return a fragment for each of the two
        // primary sections of the app.
        mSectionsPagerAdapter = SectionsPagerAdapter(
            getSupportFragmentManager()
        )

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.pager) as ViewPager
        mViewPager.setAdapter(mSectionsPagerAdapter)

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager
            .setOnPageChangeListener(object : SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    actionBar.setSelectedNavigationItem(position)
                    //Log.i("PAGER_ACTIVITY","onPageSelected() = "+position);
                    val editor: SharedPreferences.Editor = config.edit()
                    editor.putInt("displayedTab", position)
                    editor.commit()
                }
            })

        // For each of the sections in the app, add a tab to the action bar.
        for (i in 0..<mSectionsPagerAdapter!!.count) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                actionBar.newTab()
                    .setText(mSectionsPagerAdapter!!.getPageTitle(i))
                    .setTabListener(this)
            )
        }

        //		//Log.i("PAGER_ACTIVITY","config.getInt(\"displayedTab\",1) = "+config.getInt("displayedTab",1));
        mViewPager.setCurrentItem(config.getInt("displayedTab", 0))
    }

    override fun onResume() {
        super.onResume()
        //		Log.i("PHONEMAIN","onResume()");
        playerIP = config.getString("activeEmpegIP", "none")

        //		Log.i("PHONEMAIN","saved IP: "+playerIP);
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
        //		Log.i("PHONEMAIN","onPause()");
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //		Log.i("ONACTIVITYRESULT","requestCode="+requestCode+", resultCode="+resultCode+", data="+data);
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ADD_IP_REQUEST_CODE -> if (config.getString("activeEmpegIP", "none") != "none") {
                if (screenHandler != null) {
                    screenHandler!!.removeCallbacks(sr!!)
                }

                val intent = Intent("ip-change")
                // You can also include some extra data.
                intent.putExtra("newIP", config.getString("activeEmpegIP", "none"))
                LocalBroadcastManager.getInstance(this@PhoneMain).sendBroadcast(intent)

                imageURL = "http://$playerIP/proc/empeg_screen.png"

                // send a hello msg to the empeg
                sendCommand().execute("http://$playerIP/proc/empeg_notify?button=NODATA&POPUP%201%20%20Empeg%20Remote%20configured")

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

    override fun onTabSelected(
        tab: ActionBar.Tab,
        fragmentTransaction: FragmentTransaction
    ) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.position)
    }

    override fun onTabUnselected(
        tab: ActionBar.Tab,
        fragmentTransaction: FragmentTransaction
    ) {
        //		//Log.i("PAGER_ACTIVITY","onTabUnselected(): "+tab);
    }

    override fun onTabReselected(
        tab: ActionBar.Tab,
        fragmentTransaction: FragmentTransaction
    ) {
        //		//Log.i("PAGER_ACTIVITY","onTabReselected(): "+tab);
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm) {
        override fun getItem(page: Int): Fragment {
            //			//Log.i("PAGER_ACTIVITY","getItem(): "+page);
            when (page) {
                0 -> return Remote()
                1 -> return PlaylistExplorer()
            }
            return null
        }

        val count: Int
            get() =// Show 2 total pages.
                2

        override fun getPageTitle(position: Int): CharSequence? {
            val l = Locale.getDefault()
            when (position) {
                0 -> return getString(R.string.title_section1).uppercase(l)
                1 -> return getString(R.string.title_section2).uppercase(l)
            }
            return null
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

    fun playHandler(v: View) {
        //Log.i("PHONEMAIN","playhandler() pressed...");
        //get the row the clicked button is in
        val vwParentRow: LinearLayout = v.parent as LinearLayout
        /*		//Log.i("EMPEG","vwParentRow.getChildAt(0) = "+vwParentRow.getChildAt(0));
		//Log.i("EMPEG","vwParentRow.getChildAt(1) = "+vwParentRow.getChildAt(1));
		//Log.i("EMPEG","vwParentRow.getChildAt(2) = "+vwParentRow.getChildAt(2));
		//Log.i("EMPEG","vwParentRow.getChildAt(3) = "+vwParentRow.getChildAt(3));*/
        val btnChild = vwParentRow.getChildAt(0) as ImageView
        //Log.i("PHONEMAIN","playhandler "+btnChild.getTag());
        sendCommand().execute("http://" + playerIP + btnChild.tag)
        if (doVibrate) {
            vibradora.vibrate(50)
        }
        //        vwParentRow.refreshDrawableState();       
    }

    fun plEditHandler(v: View) {
        //get the row the clicked button is in
        val vwParentRow: LinearLayout = v.parent as LinearLayout
        val btnChild = vwParentRow.getChildAt(2) as ImageView

        //Log.i("PHONEMAIN","plEdithandler "+btnChild.getTag());

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

    @SuppressLint("RestrictedApi")
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
                //				updatedScreen = BitmapFactory.decodeStream(bis);
                httpclient.getConnectionManager().shutdown()
            } catch (e: MalformedURLException) {
                //Log.i("PHONEMAIN","MalformedURLException - DownloadImageTask");
            } catch (e: IOException) {
                //Log.i("PHONEMAIN","IOException - DownloadImageTask");
                show404()
            }
            return updatedScreen
        }

        override fun onPostExecute(result: Bitmap) {
            //			//Log.d("sender", "Broadcasting message");
            val intent = Intent("screen-update")
            // You can also include some extra data.
            intent.putExtra("updatedScreen", result)
            LocalBroadcastManager.getInstance(this@PhoneMain).sendBroadcast(intent)
        }
    }

    private fun show404() {
        errorHandler = Handler(Looper.getMainLooper())
        errorHandler!!.post {
            val intent = Intent("screen-update")
            // Broadcast the 404 Bitmap
            intent.putExtra(
                "updatedScreen",
                BitmapFactory.decodeStream(
                    this@PhoneMain.getResources()
                        .openRawResource(
                            getResources().getIdentifier(
                                "com.chasinglemons.empeg:drawable/not_found",
                                null,
                                null
                            )
                        )
                )
            )
            LocalBroadcastManager.getInstance(this@PhoneMain).sendBroadcast(intent)
        }
    }

    companion object {
        protected const val ADD_IP_REQUEST_CODE: Int = 100
    }
}
