package com.chasinglemons.empeg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Point
import android.inputmethodservice.Keyboard
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import org.apache.http.client.HttpClient
import java.io.IOException
import java.net.MalformedURLException

class Remote : Fragment(), OnLongClickListener, OnPanelListener,
    OnGesturePerformedListener {
    var playerIP: String? = null
    var dataURL: String? = null
    var activity: Activity? = null
    var kpanel: KeyboardPanel? = null

    var config: SharedPreferences? = null
    var doVibrate: Boolean = false
    var vibradora: Vibrator? = null
    var empegScreen: ImageView? = null
    var empegLens: ImageView? = null
    var vfd: FrameLayout? = null
    var rowG: LinearLayout? = null
    var mWidth: Int = 0
    var mAdjustedWidth: Int = 0
    var mAdjustedHeight: Int = 0
    var activeLens: Int = 0
    private var prefListener: OnSharedPreferenceChangeListener? = null
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
    var view: View? = null
    var mKeyboard: Keyboard? = null
    var mKeyboardView: CustomKeyboardView? = null
    private var gestureLib: GestureLibrary? = null
    var gestureOverlayView: GestureOverlayView? = null

    @SuppressLint("NewApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity()

        gestureOverlayView = GestureOverlayView(activity)
        config = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)


        // Inflate the layout for this fragment
        val screenDimen = resources.configuration
        Log.i("REMOTE", "screenDimen.smallestScreenWidthDp = " + screenDimen.smallestScreenWidthDp)
        Log.i("REMOTE", "screenDimen.screenHeightDp = " + screenDimen.screenHeightDp)
        view = if (screenDimen.smallestScreenWidthDp >= 360) {
            if (screenDimen.screenHeightDp < 600) {
                inflater.inflate(R.layout.small_remote_softkeys, container, false)
            } else {
                inflater.inflate(R.layout.big_remote, container, false)
            }
        } else {
            inflater.inflate(R.layout.small_remote, container, false)
        }
        gestureOverlayView.addView(view)
        gestureOverlayView.addOnGesturePerformedListener(this)
        gestureLib = GestureLibraries.fromRawResource(activity, R.raw.gestures)
        if (!gestureLib.load()) {
            Log.i("", "NO GESTURES!!")
        }

        if (config.getString("swipeAction", "0") == "1") {
            gestureOverlayView.setGestureVisible(true)
            gestureOverlayView.setGestureColor(lensColor)
            gestureOverlayView.setUncertainGestureColor(0x00000000)
            gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE)
            gestureOverlayView.setEventsInterceptionEnabled(true)
        } else {
            gestureOverlayView.setGestureVisible(false)
        }

        vfd = view!!.findViewById<View>(R.id.vfdFrame) as FrameLayout
        rowG = view!!.findViewById<View>(R.id.row_g) as LinearLayout

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(activity).registerReceiver(
            mMessageReceiver,
            IntentFilter("screen-update")
        )
        LocalBroadcastManager.getInstance(activity).registerReceiver(
            mIPChange,
            IntentFilter("ip-change")
        )

        mKeyboard = Keyboard(activity, R.xml.keyboard)
        mKeyboardView = view!!.findViewById<View>(R.id.keyboard_view) as CustomKeyboardView
        mKeyboardView!!.keyboard = mKeyboard
        mKeyboardView!!.setOnKeyboardActionListener(BasicOnKeyboardActionListener(activity))

        kpanel = view!!.findViewById<View>(R.id.kbdPanel) as KeyboardPanel
        kpanel!!.setOnPanelListener(this)
        kpanel!!.setInterpolator(ExpoInterpolator(EasingType.Type.OUT))
        if (config.getBoolean("showKeyboard", true) == false) {
            kpanel!!.visibility = View.GONE
        }

        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        prefListener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
                if (key == "activeLens") {
                    empegScreen.setColorFilter(this.colorFilter)
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
                if (config.getString("swipeAction", "0") == "1") {
                    gestureOverlayView.setGestureVisible(true)
                    gestureOverlayView.setGestureColor(this.lensColor)
                    gestureOverlayView.setUncertainGestureColor(0x00000000)
                    gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE)
                    gestureOverlayView.setEventsInterceptionEnabled(true)
                } else {
                    gestureOverlayView.setGestureVisible(false)
                }
            }
        }
        config.registerOnSharedPreferenceChangeListener(prefListener)

        vibradora = activity!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        doVibrate = config.getBoolean("doVibrate", true)
        playerIP = config.getString("activeEmpegIP", "none")

        if (config.getBoolean("showScreen", true) == false) {
            vfd.setVisibility(View.GONE)
        }
        if (config.getBoolean("showHijack", false) == true) {
            rowG.setVisibility(View.VISIBLE)
        }

        val display = activity!!.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        mWidth = size.x
        mAdjustedWidth = (mWidth * .96f).toInt()
        mAdjustedHeight = (mAdjustedWidth * .25f).toInt()

        val initScreen = Bitmap.createScaledBitmap(
            BitmapFactory.decodeStream(
                this.resources
                    .openRawResource(
                        resources.getIdentifier(
                            "com.chasinglemons.empeg:drawable/empeg_screen_init",
                            null, null
                        )
                    )
            ), mAdjustedWidth, mAdjustedHeight, false
        )

        empegScreen = view!!.findViewById<View>(R.id.blitScreen) as ImageView
        empegScreen!!.layoutParams.height = mAdjustedHeight
        empegScreen.setColorFilter(colorFilter)
        empegScreen!!.setImageBitmap(initScreen)

        val a1 = view!!.findViewById<View>(R.id.remote_a1) as ImageView
        a1.setOnLongClickListener(this)
        val a2 = view!!.findViewById<View>(R.id.remote_a2) as ImageView
        a2.setOnLongClickListener(this)
        val a3 = view!!.findViewById<View>(R.id.remote_a3) as ImageView
        a3.setOnLongClickListener(this)
        val a4 = view!!.findViewById<View>(R.id.remote_a4) as ImageView
        a4.setOnLongClickListener(this)
        val b1 = view!!.findViewById<View>(R.id.remote_b1) as ImageView
        b1.setOnLongClickListener(this)
        val b2 = view!!.findViewById<View>(R.id.remote_b2) as ImageView
        b2.setOnLongClickListener(this)
        val b3 = view!!.findViewById<View>(R.id.remote_b3) as ImageView
        b3.setOnLongClickListener(this)
        val b4 = view!!.findViewById<View>(R.id.remote_b4) as ImageView
        b4.setOnLongClickListener(this)
        val c1 = view!!.findViewById<View>(R.id.remote_c1) as ImageView
        c1.setOnLongClickListener(this)
        val c2 = view!!.findViewById<View>(R.id.remote_c2) as ImageView
        c2.setOnLongClickListener(this)
        val c3 = view!!.findViewById<View>(R.id.remote_c3) as ImageView
        c3.setOnLongClickListener(this)
        val c4 = view!!.findViewById<View>(R.id.remote_c4) as ImageView
        c4.setOnLongClickListener(this)
        val d1 = view!!.findViewById<View>(R.id.remote_d1) as ImageView
        d1.setOnLongClickListener(this)
        val d2 = view!!.findViewById<View>(R.id.remote_d2) as ImageView
        d2.setOnLongClickListener(this)
        val d3 = view!!.findViewById<View>(R.id.remote_d3) as ImageView
        d3.setOnLongClickListener(this)
        val d4 = view!!.findViewById<View>(R.id.remote_d4) as ImageView
        d4.setOnLongClickListener(this)
        val e1 = view!!.findViewById<View>(R.id.remote_e1) as ImageView
        e1.setOnLongClickListener(this)
        val e2 = view!!.findViewById<View>(R.id.remote_e2) as ImageView
        e2.setOnLongClickListener(this)
        val e3 = view!!.findViewById<View>(R.id.remote_e3) as ImageView
        e3.setOnLongClickListener(this)
        val e4 = view!!.findViewById<View>(R.id.remote_e4) as ImageView
        e4.setOnLongClickListener(this)
        val f1 = view!!.findViewById<View>(R.id.remote_f1) as ImageView
        f1.setOnLongClickListener(this)
        val f2 = view!!.findViewById<View>(R.id.remote_f2) as ImageView
        f2.setOnLongClickListener(this)
        val f3 = view!!.findViewById<View>(R.id.remote_f3) as ImageView
        f3.setOnLongClickListener(this)
        val f4 = view!!.findViewById<View>(R.id.remote_f4) as ImageView
        f4.setOnLongClickListener(this)

        return gestureOverlayView
    }

    /*	@Override
	protected void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	  super.onDestroy();
	}*/
    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.remote_a1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=One.L")
        } else if (v.id == R.id.remote_a2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Two.L")
        } else if (v.id == R.id.remote_a3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Three.L")
        } else if (v.id == R.id.remote_a4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Source.L")
        } else if (v.id == R.id.remote_b1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Four.L")
        } else if (v.id == R.id.remote_b2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Five.L")
        } else if (v.id == R.id.remote_b3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Six.L")
        } else if (v.id == R.id.remote_b4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Tuner.L")
        } else if (v.id == R.id.remote_c1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Seven.L")
        } else if (v.id == R.id.remote_c2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Eight.L")
        } else if (v.id == R.id.remote_c3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Nine.L")
        } else if (v.id == R.id.remote_c4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=SelectMode.L")
        } else if (v.id == R.id.remote_d1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Cancel.L")
        } else if (v.id == R.id.remote_d2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Zero.L")
        } else if (v.id == R.id.remote_d3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Search.L")
        } else if (v.id == R.id.remote_d4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Sound.L")
        } else if (v.id == R.id.remote_e1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Prev.L")
        } else if (v.id == R.id.remote_e2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Next.L")
        } else if (v.id == R.id.remote_e3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Menu.L")
        } else if (v.id == R.id.remote_e4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=VolUp.L")
        } else if (v.id == R.id.remote_f1) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Info.L")
        } else if (v.id == R.id.remote_f2) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Visual.L")
        } else if (v.id == R.id.remote_f3) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=Play.L")
        } else if (v.id == R.id.remote_f4) {
            sendButton().execute("http://$playerIP/proc/empeg_notify?button=VolDown.L")
        }
        if (doVibrate) {
            vibradora.vibrate(50)
        }
        return true
    }

    inner class sendButton : AsyncTask<String?, String?, String?>() {
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
                //Log.i("EMPEG","MalformedURLException");
            } catch (e: IOException) {
                //Log.i("EMPEG","IOException");
            }
            return responseBody
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: String) {
            //			//Log.i("EMPEG","onPostExecute: "+result);
        }
    }

    private val colorFilter: ColorFilter?
        get() {
            activeLens = config.getString("activeLens", "0").toInt()
            return when (activeLens) {
                0 -> ColorMatrixColorFilter(blueMatrix)
                1 -> ColorMatrixColorFilter(redMatrix)
                2 -> ColorMatrixColorFilter(yellowMatrix)
                3 -> ColorMatrixColorFilter(greenMatrix)
                4 -> null
                else -> ColorMatrixColorFilter(blueMatrix)
            }
        }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcast.
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            if (intent.getParcelableExtra<Parcelable>("updatedScreen") != null) {
                val updatedScreen = Bitmap.createScaledBitmap(
                    (intent.getParcelableExtra<Parcelable>("updatedScreen") as Bitmap?)!!,
                    mAdjustedWidth,
                    mAdjustedHeight,
                    false
                )
                empegScreen!!.setImageBitmap(updatedScreen)
            }
        }
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcast.
    private val mIPChange: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //			//Log.d("REMOTE", "Got newIP");
            // Get extra data included in the Intent
            playerIP = intent.getStringExtra("newIP")
        }
    }

    override fun onPanelClosed(panel: KeyboardPanel?) {
        /*		String panelName = getResources().getResourceEntryName(panel.getId());
		Log.d("TestPanels", "Panel [" + panelName + "] closed");*/
    }

    override fun onPanelOpened(panel: KeyboardPanel?) {
        /*		String panelName = getResources().getResourceEntryName(panel.getId());
		Log.d("TestPanels", "Panel [" + panelName + "] opened");*/
    }

    private val lensColor: Int
        get() {
            activeLens = config.getString("activeLens", "0").toInt()
            return when (activeLens) {
                0 -> -0xcc4a1b
                1 -> -0x10000
                2 -> -0x100
                3 -> -0xff0100
                4 -> -0x1
                else -> -0xcc4a1b
            }
        }

    override fun onGesturePerformed(overlay: GestureOverlayView, gesture: Gesture) {
        if (config.getString(
                "swipeAction",
                "0"
            ) == "1"
        ) { // only pay attention if gestures are 'enabled'
            val predictions: ArrayList<Prediction> = gestureLib.recognize(gesture)
            val prediction: Prediction = predictions[0] // only use the first prediction
            if (prediction.score > 1.0) {
                Log.i("WTF", "prediction.name = " + prediction.name)
                // Toast.makeText(activity.getApplicationContext(), prediction.name, Toast.LENGTH_SHORT).show();
                if (prediction.name == "up") {
                    sendButton().execute("http://$playerIP/proc/empeg_notify?button=Top")
                } else if (prediction.name == "down") {
                    sendButton().execute("http://$playerIP/proc/empeg_notify?button=Bottom")
                } else if (prediction.name == "left") {
                    sendButton().execute("http://$playerIP/proc/empeg_notify?button=Left")
                } else if (prediction.name == "right") {
                    sendButton().execute("http://$playerIP/proc/empeg_notify?button=Right")
                }
            }
            /*for (Prediction prediction : predictions) {
				if (prediction.score > 1.0) {
					Log.i("WTF","prediction.name = "+prediction.name);
					// Toast.makeText(activity.getApplicationContext(), prediction.name, Toast.LENGTH_SHORT).show();
					if (prediction.name.equals("up")) {
						new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Top");
					} else if (prediction.name.equals("down")) {
						new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Bottom");
					} else if (prediction.name.equals("left")) {
						new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Left");
					} else if (prediction.name.equals("right")) {
						new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Right");
					}
				}
			}*/
        }
    }

    companion object {
        const val MENU_DISCOVERY: Int = Menu.FIRST
        const val MENU_PLAYLISTS: Int = Menu.FIRST + 1
        const val MENU_WIDGETS: Int = Menu.FIRST + 2
        const val MENU_SETTINGS: Int = Menu.FIRST + 3
    }
}