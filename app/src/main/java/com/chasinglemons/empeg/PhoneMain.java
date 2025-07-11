package com.chasinglemons.empeg;

import static java.net.HttpURLConnection.setFollowRedirects;

import android.annotation.SuppressLint;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import io.ktor.client.engine.okhttp.OkHttpConfig;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import io.ktor.client.HttpClient;
import io.ktor.client.engine.okhttp.OkHttp;
import io.ktor.client.engine.okhttp.OkHttpConfig;

public class PhoneMain extends FragmentActivity implements ActionBar.TabListener, OnSharedPreferenceChangeListener {

	protected static final int ADD_IP_REQUEST_CODE = 100;
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	String playerIP;
	SharedPreferences config;
	boolean doVibrate;
	Vibrator vibradora;
	Handler screenHandler,errorHandler;
	Runnable sr;
	String imageURL,enqueueURL,appendURL,insertURL,streamURL;
	int screenRefreshRate = 1000;
	boolean doScreenUpdate = true;
	private OnSharedPreferenceChangeListener prefListener;
	QuickAction mQuickAction;
	int mAdjustedWidth,mAdjustedHeight;
	CustomViewPager cPager;
	HttpClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phone_main);

		// Implement Quick Action here.
		ActionItem enqueueAction = new ActionItem();
		enqueueAction.setTitle("Enqueue");
		ActionItem appendAction = new ActionItem();
		appendAction.setTitle("Append");
		ActionItem insertAction = new ActionItem();
		insertAction.setTitle("Insert");
		ActionItem streamAction = new ActionItem();
		streamAction.setTitle("Stream");
		mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(enqueueAction);
		mQuickAction.addActionItem(appendAction);
		mQuickAction.addActionItem(insertAction);
		mQuickAction.addActionItem(streamAction);

		// setup the action item click listener
		mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(int pos) {
				if (pos == 0) { // enqueue selected
					sendCommand("http://"+playerIP+enqueueURL);

				} else if (pos == 1) { // append selected
					sendCommand("http://"+playerIP+appendURL);

				} else if (pos == 2) { // insert selected
					sendCommand("http://"+playerIP+insertURL);

				} else if (pos == 3) { // stream selected
					Intent intent = new Intent();  
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("http://"+playerIP+streamURL), "audio/*");
					startActivity(intent);
				}
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});

		config = PreferenceManager.getDefaultSharedPreferences(this);

		vibradora = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		doVibrate = config.getBoolean("doVibrate", true);
		doScreenUpdate = config.getBoolean("doScreenUpdate", true);
		screenRefreshRate = Integer.parseInt(config.getString("refreshRate", "1000"));

		if (config.getString("activeEmpegIP", "none").equals("none")) {
			startActivityForResult(new Intent(this,AddEmpeg.class), ADD_IP_REQUEST_CODE);
		} else {
			playerIP = config.getString("activeEmpegIP", "none");
			if (config.getBoolean("doNotifications", true)) {
				Intent service = new Intent(this, NotificationService.class);
				this.startService(service); 
			}
		}

		cPager = (CustomViewPager) findViewById(R.id.pager);
		if (config.getString("swipeAction", "0").equals("1")) { // disable viewpager swiping
			cPager.setPagingEnabled(false);
		}

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mAdjustedWidth = (int) (size.x*.96F);
		mAdjustedHeight = (int) (mAdjustedWidth*.25F);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		prefListener = new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				doScreenUpdate = config.getBoolean("doScreenUpdate", true);
				if (key.equals("doScreenUpdate")) {
					if (!doScreenUpdate) {
						Intent intent = new Intent("screen-update");
						Bitmap initScreen = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(getResources()
								.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/empeg_screen_init",
										null,null))),mAdjustedWidth,mAdjustedHeight,false);
						intent.putExtra("updatedScreen",initScreen);
						LocalBroadcastManager.getInstance(PhoneMain.this).sendBroadcast(intent);
					}
				}
				if (key.equals("refreshRate")) {
					screenRefreshRate = Integer.parseInt(config.getString("refreshRate", "1000"));
				}
				if (config.getBoolean("doNotifications", true) == false) {
					Intent service = new Intent(getApplicationContext(), NotificationService.class);
					stopService(service);
				} else {
					Intent service = new Intent(getApplicationContext(), NotificationService.class);
					startService(service);
				}
				if (key.equals("swipeAction")) {
					if (config.getString("swipeAction", "0").equals("1")) {
						cPager.setPagingEnabled(false); // disable viewpager swiping
						
					} else {
						cPager.setPagingEnabled(true);
					}
				}
			}
		};
		config.registerOnSharedPreferenceChangeListener(prefListener);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		/*		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);*/
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the two
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
		.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
				//Log.i("PAGER_ACTIVITY","onPageSelected() = "+position);
				SharedPreferences.Editor editor = config.edit();
				editor.putInt("displayedTab",position);
				editor.commit();
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		//		//Log.i("PAGER_ACTIVITY","config.getInt(\"displayedTab\",1) = "+config.getInt("displayedTab",1));
		mViewPager.setCurrentItem(config.getInt("displayedTab",0));
	}

	@Override
	public void onResume() {
		super.onResume();
		//		Log.i("PHONEMAIN","onResume()");
		playerIP = config.getString("activeEmpegIP", "none");
		//		Log.i("PHONEMAIN","saved IP: "+playerIP);

		if (!playerIP.equals("none")) {
			imageURL = "http://"+playerIP+"/proc/empeg_screen.png";
			if (config.getBoolean("doScreenUpdate", true) == true && config.getBoolean("showScreen", true) == true) {
				refreshScreen();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		//		Log.i("PHONEMAIN","onPause()");
		if (screenHandler != null) {
			screenHandler.removeCallbacks(sr);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Intent service = new Intent(this, NotificationService.class);
		stopService(service);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//		Log.i("ONACTIVITYRESULT","requestCode="+requestCode+", resultCode="+resultCode+", data="+data);
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ADD_IP_REQUEST_CODE:
			if (!config.getString("activeEmpegIP", "none").equals("none")) {
				if (screenHandler != null) {
					screenHandler.removeCallbacks(sr);
				}

				Intent intent = new Intent("ip-change");
				// You can also include some extra data.
				intent.putExtra("newIP",config.getString("activeEmpegIP", "none"));
				LocalBroadcastManager.getInstance(PhoneMain.this).sendBroadcast(intent);

				imageURL = "http://"+playerIP+"/proc/empeg_screen.png";

				// send a hello msg to the empeg
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=NODATA&POPUP%201%20%20Empeg%20Remote%20configured");

				refreshScreen();

				if (config.getBoolean("doNotifications", true)) {
					Intent service = new Intent(this, NotificationService.class);
					this.startService(service); 
				}

			} else {
				finish();
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_discovery) {
			startActivityForResult(new Intent(this,AddEmpeg.class), ADD_IP_REQUEST_CODE);
			return true;
		} else if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(this, Settings.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key != null) {
			//Log.i("PAGER_ACTIVITY","onSharedPreferenceChanged: key: "+key);
		}
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());

	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		//		//Log.i("PAGER_ACTIVITY","onTabUnselected(): "+tab);
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		//		//Log.i("PAGER_ACTIVITY","onTabReselected(): "+tab);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int page) {
			//			//Log.i("PAGER_ACTIVITY","getItem(): "+page);
			switch (page) {
			case 0: return new Remote();
			case 1: return new PlaylistExplorer();
			//and so on....
			}
			return null;
		}

		@Override
		public int getCount() {
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}

	public static String sendCommand(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuilder response = new StringBuilder();

		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET"); // Set the request method to GET

			int responseCode = connection.getResponseCode(); // Get the HTTP response code
			System.out.println("Response Code: " + responseCode);

			if (responseCode == HttpURLConnection.HTTP_OK) { // Check if the request was successful (HTTP_OK)
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
			} else {
				// Handle non-200 responses here
				System.out.println("Error: " + connection.getResponseMessage());
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
			if (connection != null) {
				connection.disconnect(); // Disconnect the connection to release resources
			}
		}
		return response.toString();
	}

	public void buttonPress(View v) throws IOException {
		if (v.getId() == R.id.remote_a1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=One");
		} else if (v.getId() == R.id.remote_a2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Two");
		} else if (v.getId() == R.id.remote_a3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Three");
		} else if (v.getId() == R.id.remote_a4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Source");
		} else if (v.getId() == R.id.remote_b1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Four");
		} else if (v.getId() == R.id.remote_b2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Five");
		} else if (v.getId() == R.id.remote_b3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Six");
		} else if (v.getId() == R.id.remote_b4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Tuner");
		} else if (v.getId() == R.id.remote_c1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Seven");
		} else if (v.getId() == R.id.remote_c2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Eight");
		} else if (v.getId() == R.id.remote_c3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Nine");
		} else if (v.getId() == R.id.remote_c4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=SelectMode");
		} else if (v.getId() == R.id.remote_d1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
		} else if (v.getId() == R.id.remote_d2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Zero");
		} else if (v.getId() == R.id.remote_d3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Search");
		} else if (v.getId() == R.id.remote_d4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Sound");
		} else if (v.getId() == R.id.remote_e1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Prev");
		} else if (v.getId() == R.id.remote_e2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Next");
		} else if (v.getId() == R.id.remote_e3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Menu");
		} else if (v.getId() == R.id.remote_e4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=KnobRight");
		} else if (v.getId() == R.id.remote_f1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Info");
		} else if (v.getId() == R.id.remote_f2) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Visual");
		} else if (v.getId() == R.id.remote_f3) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Play");
		} else if (v.getId() == R.id.remote_f4) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=KnobLeft");
		} else if (v.getId() == R.id.remote_g1) {
			sendCommand("http://"+playerIP+"/proc/empeg_notify?button=HijackMenu");
		}
		if (doVibrate) {
			vibradora.vibrate(50);
		}
	}

	public void playHandler(View v) throws IOException {
		//Log.i("PHONEMAIN","playhandler() pressed...");
		//get the row the clicked button is in
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		/*		//Log.i("EMPEG","vwParentRow.getChildAt(0) = "+vwParentRow.getChildAt(0));
		//Log.i("EMPEG","vwParentRow.getChildAt(1) = "+vwParentRow.getChildAt(1));
		//Log.i("EMPEG","vwParentRow.getChildAt(2) = "+vwParentRow.getChildAt(2));
		//Log.i("EMPEG","vwParentRow.getChildAt(3) = "+vwParentRow.getChildAt(3));*/
		ImageView btnChild = (ImageView)vwParentRow.getChildAt(0);
		//Log.i("PHONEMAIN","playhandler "+btnChild.getTag());
		sendCommand("http://"+playerIP+btnChild.getTag());
		if (doVibrate) {
			vibradora.vibrate(50);
		}
		//        vwParentRow.refreshDrawableState();       
	}

	public void plEditHandler(View v) {
		//get the row the clicked button is in
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		ImageView btnChild = (ImageView)vwParentRow.getChildAt(2);
		//Log.i("PHONEMAIN","plEdithandler "+btnChild.getTag());

		// explode tag
		String[] tagURLs = btnChild.getTag().toString().split(":");
		enqueueURL = tagURLs[0];
		appendURL = tagURLs[1];
		insertURL = tagURLs[2];
		streamURL = tagURLs[3];

		// show quickaction now that the URLs are populated
		mQuickAction.show(v);

		if (doVibrate) {
			vibradora.vibrate(50);
		}
	}

	@SuppressLint("RestrictedApi")
    @Override
	public boolean dispatchKeyEvent(KeyEvent e) {

		if (e.getAction() == KeyEvent.ACTION_DOWN) {
			if (e.getKeyCode() == KeyEvent.KEYCODE_A || e.getKeyCode() == KeyEvent.KEYCODE_B || e.getKeyCode() == KeyEvent.KEYCODE_C || e.getKeyCode() == KeyEvent.KEYCODE_2) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Two");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_D || e.getKeyCode() == KeyEvent.KEYCODE_E || e.getKeyCode() == KeyEvent.KEYCODE_F || e.getKeyCode() == KeyEvent.KEYCODE_3) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Three");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_G || e.getKeyCode() == KeyEvent.KEYCODE_H || e.getKeyCode() == KeyEvent.KEYCODE_I || e.getKeyCode() == KeyEvent.KEYCODE_4) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Four");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_J || e.getKeyCode() == KeyEvent.KEYCODE_K || e.getKeyCode() == KeyEvent.KEYCODE_L || e.getKeyCode() == KeyEvent.KEYCODE_5) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Five");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_M || e.getKeyCode() == KeyEvent.KEYCODE_N || e.getKeyCode() == KeyEvent.KEYCODE_O || e.getKeyCode() == KeyEvent.KEYCODE_6) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Six");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_P || e.getKeyCode() == KeyEvent.KEYCODE_R || e.getKeyCode() == KeyEvent.KEYCODE_S || e.getKeyCode() == KeyEvent.KEYCODE_7) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Seven");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_T || e.getKeyCode() == KeyEvent.KEYCODE_U || e.getKeyCode() == KeyEvent.KEYCODE_V || e.getKeyCode() == KeyEvent.KEYCODE_8) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Eight");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_W || e.getKeyCode() == KeyEvent.KEYCODE_X || e.getKeyCode() == KeyEvent.KEYCODE_Y || e.getKeyCode() == KeyEvent.KEYCODE_9) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Nine");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_Q || e.getKeyCode() == KeyEvent.KEYCODE_Z || e.getKeyCode() == KeyEvent.KEYCODE_0) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Zero");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				KeyboardPanel.setClosed();
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Menu");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				sendCommand("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
				return true;
			}
			if (doVibrate) {
				vibradora.vibrate(50);
			}
		}
		return super.dispatchKeyEvent(e);
	}

	private void refreshScreen() {
		screenHandler = new Handler();
		sr = new Runnable() {

			@Override
			public void run() {
				if (doScreenUpdate) {
					//					//Log.i("","trying "+imageURL);
					new DownloadImageTask().execute(imageURL);
					screenHandler.postDelayed(sr,screenRefreshRate);
				}
			}
		};
		screenHandler.post(sr);
	}

	private class DownloadImageTask extends AsyncTask<String,String,Bitmap> {
		@Override
		protected Bitmap doInBackground(String... urls) {
			Bitmap updatedScreen = null;
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(urls[0]);
				HttpResponse response = httpclient.execute(httpget);
				InputStream in = response.getEntity().getContent();
				BufferedInputStream bis = new BufferedInputStream(in, 8192);
				updatedScreen = BitmapFactory.decodeStream(bis);
				//				updatedScreen = BitmapFactory.decodeStream(bis);
				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				//Log.i("PHONEMAIN","MalformedURLException - DownloadImageTask");
			} catch (IOException e) {
				//Log.i("PHONEMAIN","IOException - DownloadImageTask");
				show404();
			}
			return updatedScreen;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			//			//Log.d("sender", "Broadcasting message");
			Intent intent = new Intent("screen-update");
			// You can also include some extra data.
			intent.putExtra("updatedScreen",result);
			LocalBroadcastManager.getInstance(PhoneMain.this).sendBroadcast(intent);
		}
	}

	private void show404() {
		errorHandler = new Handler(Looper.getMainLooper());
		errorHandler.post(new Runnable(){

			@Override
			public void run() {
				Intent intent = new Intent("screen-update");
				// Broadcast the 404 Bitmap
				intent.putExtra("updatedScreen",BitmapFactory.decodeStream(PhoneMain.this.getResources()
						.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/not_found",null,null))));
				LocalBroadcastManager.getInstance(PhoneMain.this).sendBroadcast(intent);
			}
		});
	}
}
