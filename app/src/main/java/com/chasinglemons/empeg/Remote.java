package com.chasinglemons.empeg;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.miscwidgets.interpolator.EasingType.Type;
import org.miscwidgets.interpolator.ExpoInterpolator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.inputmethodservice.Keyboard;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chasinglemons.empeg.KeyboardPanel.OnPanelListener;

public class Remote extends Fragment implements OnLongClickListener,OnPanelListener,OnGesturePerformedListener {

	String playerIP;
	String dataURL;
	Activity activity;
	KeyboardPanel kpanel;

	SharedPreferences config;
	public static final int MENU_DISCOVERY = Menu.FIRST;
	public static final int MENU_PLAYLISTS = Menu.FIRST + 1;
	public static final int MENU_WIDGETS = Menu.FIRST + 2;
	public static final int MENU_SETTINGS = Menu.FIRST + 3;
	boolean doVibrate;
	Vibrator vibradora;
	ImageView empegScreen,empegLens;
	FrameLayout vfd;
	LinearLayout rowG;
	int mWidth,mAdjustedWidth,mAdjustedHeight;
	int activeLens = 0;
	private OnSharedPreferenceChangeListener prefListener;
	float[] redMatrix = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0};
	float[] greenMatrix = {0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0};
	float[] blueMatrix = {0,0,0,0,0,0,0.4f,0.35f,0,0,0,0,1,0,0,0,0.1f,1,0,0};
	float[] yellowMatrix = { 
			1, 0, 0, 0, 0, //red
			0, 1, 0, 0, 0, //green
			0, 0, 0, 0, 0, //blue
			0.5f, 0.5f, 0, 0, 0 //alpha
	};
	View view;
	Keyboard mKeyboard;
	CustomKeyboardView mKeyboardView;
	private GestureLibrary gestureLib;
	GestureOverlayView gestureOverlayView;

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		
		activity = getActivity();
		
		gestureOverlayView = new GestureOverlayView(activity);
		config = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
		
		// Inflate the layout for this fragment
		Configuration screenDimen = getResources().getConfiguration();
		Log.i("REMOTE","screenDimen.smallestScreenWidthDp = "+screenDimen.smallestScreenWidthDp);
		Log.i("REMOTE","screenDimen.screenHeightDp = "+screenDimen.screenHeightDp);
		if (screenDimen.smallestScreenWidthDp >= 360) {
			if (screenDimen.screenHeightDp < 600) {
				view = inflater.inflate(R.layout.small_remote_softkeys, container, false);
			} else {
				view = inflater.inflate(R.layout.big_remote, container, false);
			}
		} else {
			view = inflater.inflate(R.layout.small_remote, container, false);
		}
		gestureOverlayView.addView(view);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(activity, R.raw.gestures);
	    if (!gestureLib.load()) {
	      Log.i("","NO GESTURES!!");
	    }
	    
	    if (config.getString("swipeAction", "0").equals("1")) {
			gestureOverlayView.setGestureVisible(true);
			gestureOverlayView.setGestureColor(getLensColor());
			gestureOverlayView.setUncertainGestureColor(0x00000000);
			gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE);
			gestureOverlayView.setEventsInterceptionEnabled(true);
		} else {
			gestureOverlayView.setGestureVisible(false);
		}

		vfd = (FrameLayout) view.findViewById(R.id.vfdFrame);
		rowG = (LinearLayout) view.findViewById(R.id.row_g);

		// Register to receive messages.
		// We are registering an observer (mMessageReceiver) to receive Intents
		// with actions named "custom-event-name".
		LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver,
				new IntentFilter("screen-update"));
		LocalBroadcastManager.getInstance(activity).registerReceiver(mIPChange,
				new IntentFilter("ip-change"));

		mKeyboard = new Keyboard(activity, R.xml.keyboard);
		mKeyboardView = (CustomKeyboardView) view.findViewById(R.id.keyboard_view);
		mKeyboardView.setKeyboard(mKeyboard);
		mKeyboardView.setOnKeyboardActionListener(new BasicOnKeyboardActionListener(activity));

		kpanel = (KeyboardPanel) view.findViewById(R.id.kbdPanel);
		kpanel.setOnPanelListener(this);
		kpanel.setInterpolator(new ExpoInterpolator(Type.OUT));
		if (config.getBoolean("showKeyboard", true) == false) {
			kpanel.setVisibility(View.GONE);
		}

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				if (key.equals("activeLens")) {
					empegScreen.setColorFilter(getColorFilter());
				}
				if (config.getBoolean("showScreen", true) == false) {
					vfd.setVisibility(View.GONE);
				} else {
					vfd.setVisibility(View.VISIBLE);
				}
				if (config.getBoolean("showHijack", false) == false) {
					rowG.setVisibility(View.GONE);
				} else {
					rowG.setVisibility(View.VISIBLE);
				}
				if (config.getBoolean("showKeyboard", false) == false) {
					kpanel.setVisibility(View.GONE);
				} else {
					kpanel.setVisibility(View.VISIBLE);
				}
				if (config.getString("swipeAction", "0").equals("1")) {
					gestureOverlayView.setGestureVisible(true);
					gestureOverlayView.setGestureColor(getLensColor());
					gestureOverlayView.setUncertainGestureColor(0x00000000);
					gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE);
					gestureOverlayView.setEventsInterceptionEnabled(true);
				} else {
					gestureOverlayView.setGestureVisible(false);
				}
			}
		};
		config.registerOnSharedPreferenceChangeListener(prefListener);

		vibradora = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
		doVibrate = config.getBoolean("doVibrate", true);
		playerIP = config.getString("activeEmpegIP", "none");

		if (config.getBoolean("showScreen", true) == false) {
			vfd.setVisibility(View.GONE);
		}
		if (config.getBoolean("showHijack", false) == true) {
			rowG.setVisibility(View.VISIBLE);
		}

		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mWidth = size.x;
		mAdjustedWidth = (int) (mWidth*.96F);
		mAdjustedHeight = (int) (mAdjustedWidth*.25F);

		Bitmap initScreen = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(this.getResources()
				.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/empeg_screen_init",
						null,null))),mAdjustedWidth,mAdjustedHeight,false);

		empegScreen = (ImageView) view.findViewById(R.id.blitScreen);
		empegScreen.getLayoutParams().height = mAdjustedHeight;
		empegScreen.setColorFilter(getColorFilter());
		empegScreen.setImageBitmap(initScreen);

		ImageView a1 = (ImageView) view.findViewById(R.id.remote_a1);
		a1.setOnLongClickListener(this);
		ImageView a2 = (ImageView) view.findViewById(R.id.remote_a2);
		a2.setOnLongClickListener(this);
		ImageView a3 = (ImageView) view.findViewById(R.id.remote_a3);
		a3.setOnLongClickListener(this);
		ImageView a4 = (ImageView) view.findViewById(R.id.remote_a4);
		a4.setOnLongClickListener(this);
		ImageView b1 = (ImageView) view.findViewById(R.id.remote_b1);
		b1.setOnLongClickListener(this);
		ImageView b2 = (ImageView) view.findViewById(R.id.remote_b2);
		b2.setOnLongClickListener(this);
		ImageView b3 = (ImageView) view.findViewById(R.id.remote_b3);
		b3.setOnLongClickListener(this);
		ImageView b4 = (ImageView) view.findViewById(R.id.remote_b4);
		b4.setOnLongClickListener(this);
		ImageView c1 = (ImageView) view.findViewById(R.id.remote_c1);
		c1.setOnLongClickListener(this);
		ImageView c2 = (ImageView) view.findViewById(R.id.remote_c2);
		c2.setOnLongClickListener(this);
		ImageView c3 = (ImageView) view.findViewById(R.id.remote_c3);
		c3.setOnLongClickListener(this);
		ImageView c4 = (ImageView) view.findViewById(R.id.remote_c4);
		c4.setOnLongClickListener(this);
		ImageView d1 = (ImageView) view.findViewById(R.id.remote_d1);
		d1.setOnLongClickListener(this);
		ImageView d2 = (ImageView) view.findViewById(R.id.remote_d2);
		d2.setOnLongClickListener(this);
		ImageView d3 = (ImageView) view.findViewById(R.id.remote_d3);
		d3.setOnLongClickListener(this);
		ImageView d4 = (ImageView) view.findViewById(R.id.remote_d4);
		d4.setOnLongClickListener(this);
		ImageView e1 = (ImageView) view.findViewById(R.id.remote_e1);
		e1.setOnLongClickListener(this);
		ImageView e2 = (ImageView) view.findViewById(R.id.remote_e2);
		e2.setOnLongClickListener(this);
		ImageView e3 = (ImageView) view.findViewById(R.id.remote_e3);
		e3.setOnLongClickListener(this);
		ImageView e4 = (ImageView) view.findViewById(R.id.remote_e4);
		e4.setOnLongClickListener(this);
		ImageView f1 = (ImageView) view.findViewById(R.id.remote_f1);
		f1.setOnLongClickListener(this);
		ImageView f2 = (ImageView) view.findViewById(R.id.remote_f2);
		f2.setOnLongClickListener(this);
		ImageView f3 = (ImageView) view.findViewById(R.id.remote_f3);
		f3.setOnLongClickListener(this);
		ImageView f4 = (ImageView) view.findViewById(R.id.remote_f4);
		f4.setOnLongClickListener(this);

		return gestureOverlayView;
	}

	/*	@Override
	protected void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	  super.onDestroy();
	}*/

	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()) {
		case R.id.remote_a1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=One.L");
			break;
		case R.id.remote_a2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Two.L");
			break;
		case R.id.remote_a3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Three.L");
			break;
		case R.id.remote_a4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Source.L");
			break;
		case R.id.remote_b1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Four.L");
			break;
		case R.id.remote_b2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Five.L");
			break;
		case R.id.remote_b3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Six.L");
			break;
		case R.id.remote_b4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Tuner.L");
			break;
		case R.id.remote_c1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Seven.L");
			break;
		case R.id.remote_c2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Eight.L");
			break;
		case R.id.remote_c3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Nine.L");
			break;
		case R.id.remote_c4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=SelectMode.L");
			break;
		case R.id.remote_d1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
			break;
		case R.id.remote_d2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Zero.L");
			break;
		case R.id.remote_d3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Search.L");
			break;
		case R.id.remote_d4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Sound.L");
			break;
		case R.id.remote_e1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Prev.L");
			break;
		case R.id.remote_e2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Next.L");
			break;
		case R.id.remote_e3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Menu.L");
			break;
		case R.id.remote_e4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=VolUp.L");
			break;
		case R.id.remote_f1:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Info.L");
			break;
		case R.id.remote_f2:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Visual.L");
			break;
		case R.id.remote_f3:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Play.L");
			break;
		case R.id.remote_f4:
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=VolDown.L");
			break;
		}
		if (doVibrate) {
			vibradora.vibrate(50);
		}
		return true;
	}

	public class sendButton extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... url) {
			String responseBody = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				//				//Log.i("EMPEG","FETCHING: "+url[0]);
				HttpGet httpget = new HttpGet(url[0]);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				responseBody = httpclient.execute(httpget, responseHandler);

				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				//Log.i("EMPEG","MalformedURLException");
			} catch (IOException e) {
				//Log.i("EMPEG","IOException");
			}
			return responseBody;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
		}

		@Override
		protected void onPostExecute(String result) {
			//			//Log.i("EMPEG","onPostExecute: "+result);
		}
	}

	private ColorFilter getColorFilter() {
		activeLens = Integer.parseInt(config.getString("activeLens","0"));
		switch (activeLens) {
		case 0:
			return new ColorMatrixColorFilter(blueMatrix);
		case 1:
			return new ColorMatrixColorFilter(redMatrix);
		case 2:
			return new ColorMatrixColorFilter(yellowMatrix);
		case 3:
			return new ColorMatrixColorFilter(greenMatrix);
		case 4:
			return null;
		default:
			return new ColorMatrixColorFilter(blueMatrix);
		}
	}

	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcast.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			if (intent.getParcelableExtra("updatedScreen") != null) {
				Bitmap updatedScreen = Bitmap.createScaledBitmap((Bitmap) intent.getParcelableExtra("updatedScreen"),mAdjustedWidth,mAdjustedHeight,false);
				empegScreen.setImageBitmap(updatedScreen);
			}	
		}
	};
	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcast.
	private BroadcastReceiver mIPChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//			//Log.d("REMOTE", "Got newIP");
			// Get extra data included in the Intent
			playerIP = intent.getStringExtra("newIP");
		}
	};

	@Override
	public void onPanelClosed(KeyboardPanel panel) {
		/*		String panelName = getResources().getResourceEntryName(panel.getId());
		Log.d("TestPanels", "Panel [" + panelName + "] closed");*/
	}
	@Override
	public void onPanelOpened(KeyboardPanel panel) {
		/*		String panelName = getResources().getResourceEntryName(panel.getId());
		Log.d("TestPanels", "Panel [" + panelName + "] opened");*/
	}
	
	private int getLensColor() {
		activeLens = Integer.parseInt(config.getString("activeLens","0"));
		switch (activeLens) {
		case 0:
			return 0xFF33B5E5;
		case 1:
			return 0xFFFF0000;
		case 2:
			return 0xFFFFFF00;
		case 3:
			return 0xFF00FF00;
		case 4:
			return 0xFFFFFFFF;
		default:
			return 0xFF33B5E5;
		}
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		if (config.getString("swipeAction", "0").equals("1")) { // only pay attention if gestures are 'enabled'
			ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
			Prediction prediction = predictions.get(0); // only use the first prediction
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
}