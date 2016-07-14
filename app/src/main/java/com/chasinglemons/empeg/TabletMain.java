package com.chasinglemons.empeg;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.miscwidgets.interpolator.ExpoInterpolator;
import org.miscwidgets.interpolator.EasingType.Type;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.chasinglemons.empeg.KeyboardPanel.OnPanelListener;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;

public class TabletMain extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener,OnLongClickListener,OnPanelListener {

	protected static final int ADD_IP_REQUEST_CODE = 100;
	private String playerIP;
	private SharedPreferences config;
	private boolean doVibrate;
	private Vibrator vibradora;
	private Handler screenHandler,errorHandler;
	private Runnable sr;
	private String imageURL,enqueueURL,appendURL,insertURL,streamURL;
	private int screenRefreshRate = 1000;
	private boolean doScreenUpdate = true;
	private OnSharedPreferenceChangeListener prefListener;
	private QuickAction mQuickAction;

	// Playlist Explorer
	private ProgressBar progresso;
	private GlobalData gData;
	private Button pHistoryButton,pHomeButton;
	private LinearLayout pHistoryLayout,pScroller,pNotFound;
	private ListView lview;

	// Remote
	private String dataURL;
	public static final int MENU_DISCOVERY = Menu.FIRST;
	public static final int MENU_PLAYLISTS = Menu.FIRST + 1;
	public static final int MENU_WIDGETS = Menu.FIRST + 2;
	public static final int MENU_SETTINGS = Menu.FIRST + 3;
	private ImageView empegScreen, empegLens;
	private FrameLayout vfd;
	private LinearLayout rowG;
	private int mWidth,mAdjustedWidth,mAdjustedHeight;
	private int activeLens = 0;
	private float[] redMatrix = {
			1, 0, 0, 0, 0,
			0, 0, 0, 0, 0,
			0, 0, 0, 0, 0,
			1, 0, 0, 0, 0};
	private float[] greenMatrix = {
			0, 0, 0, 0, 0,
			0, 1, 0, 0, 0,
			0, 0, 0, 0, 0,
			0, 1, 0, 0, 0};
	private float[] blueMatrix = {
			0, 0, 0, 0, 0,
			0, 0.4f, 0.35f, 0, 0,
			0, 0, 1, 0, 0,
			0, 0.1f, 1, 0, 0};
	private float[] yellowMatrix = {
			1, 0, 0, 0, 0, //red
			0, 1, 0, 0, 0, //green
			0, 0, 0, 0, 0, //blue
			0.5f, 0.5f, 0, 0, 0 //alpha
	};
	private Keyboard mKeyboard;
	private CustomKeyboardView mKeyboardView;
	private KeyboardPanel kpanel;
	private EditText messageInput;
	private View positiveAction;
	private MaterialNumberPicker picker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tablet_main);

		gData = ((GlobalData) getApplicationContext());

		// Implement Quick Action here.
		// A pop up will have 3  actions for user select:
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
					new sendCommand().execute("http://"+playerIP+enqueueURL);

				} else if (pos == 1) { // append selected
					new sendCommand().execute("http://"+playerIP+appendURL);

				} else if (pos == 2) { // insert selected
					new sendCommand().execute("http://"+playerIP+insertURL);

				} else if (pos == 3) { // stream selected
					Intent intent = new Intent();  
					intent.setAction(android.content.Intent.ACTION_VIEW);  
					intent.setDataAndType(Uri.parse("http://"+playerIP+streamURL), "audio/*");
					startActivity(intent);
				}
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});

		// References
		vfd = (FrameLayout) findViewById(R.id.vfdFrame);
		rowG = (LinearLayout) findViewById(R.id.row_g);
		pNotFound = (LinearLayout) findViewById(R.id.List_Not_Found);
		pScroller = (LinearLayout) findViewById(R.id.List_Scroller);
		pHistoryLayout = (LinearLayout) findViewById(R.id.List_Back);

		config = PreferenceManager.getDefaultSharedPreferences(this);
		
		mKeyboard = new Keyboard(this, R.xml.keyboard);
		mKeyboardView = (CustomKeyboardView) findViewById(R.id.keyboard_view);
		mKeyboardView.setKeyboard(mKeyboard);
		mKeyboardView.setOnKeyboardActionListener(new BasicOnKeyboardActionListener(this));

		kpanel = (KeyboardPanel) findViewById(R.id.kbdPanel);
		kpanel.setOnPanelListener(this);
		kpanel.setInterpolator(new ExpoInterpolator(Type.OUT));
		if (config.getBoolean("showKeyboard", true) == false) {
			kpanel.setVisibility(View.GONE);
		}

		vibradora = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		doVibrate = config.getBoolean("doVibrate", true);
		doScreenUpdate = config.getBoolean("doScreenUpdate", true);
		screenRefreshRate = Integer.parseInt(config.getString("refreshRate", "1000"));

		if (config.getBoolean("showScreen", true) == false) {
			vfd.setVisibility(View.GONE);
		}
		if (config.getBoolean("showHijack", false) == true) {
			rowG.setVisibility(View.VISIBLE);
		}

		if (config.getString("activeEmpegIP", "none").equals("none")) {
			startActivityForResult(new Intent(this,AddEmpeg.class), ADD_IP_REQUEST_CODE);
		} else {
			playerIP = config.getString("activeEmpegIP", "none");
			new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");
			if (config.getBoolean("doNotifications", true)) {
				Intent service = new Intent(this, NotificationService.class);
				this.startService(service); 
			}
		}

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mWidth = size.x;
		mAdjustedWidth = (int) (mWidth*.666F);
		mAdjustedHeight = (int) (mAdjustedWidth*.2F);

		Bitmap initScreen = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(this.getResources()
				.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/empeg_screen_init",
						null,null))),mAdjustedWidth,mAdjustedHeight,false);

		empegScreen = (ImageView) findViewById(R.id.blitScreen);
		empegScreen.getLayoutParams().height = mAdjustedHeight;
		empegScreen.setColorFilter(getColorFilter());
		empegScreen.setImageBitmap(initScreen);

		ImageView a1 = (ImageView) findViewById(R.id.remote_a1);
		a1.setOnLongClickListener(this);
		ImageView a2 = (ImageView) findViewById(R.id.remote_a2);
		a2.setOnLongClickListener(this);
		ImageView a3 = (ImageView) findViewById(R.id.remote_a3);
		a3.setOnLongClickListener(this);
		ImageView a4 = (ImageView) findViewById(R.id.remote_a4);
		a4.setOnLongClickListener(this);
		ImageView b1 = (ImageView) findViewById(R.id.remote_b1);
		b1.setOnLongClickListener(this);
		ImageView b2 = (ImageView) findViewById(R.id.remote_b2);
		b2.setOnLongClickListener(this);
		ImageView b3 = (ImageView) findViewById(R.id.remote_b3);
		b3.setOnLongClickListener(this);
		ImageView b4 = (ImageView) findViewById(R.id.remote_b4);
		b4.setOnLongClickListener(this);
		ImageView c1 = (ImageView) findViewById(R.id.remote_c1);
		c1.setOnLongClickListener(this);
		ImageView c2 = (ImageView) findViewById(R.id.remote_c2);
		c2.setOnLongClickListener(this);
		ImageView c3 = (ImageView) findViewById(R.id.remote_c3);
		c3.setOnLongClickListener(this);
		ImageView c4 = (ImageView) findViewById(R.id.remote_c4);
		c4.setOnLongClickListener(this);
		ImageView d1 = (ImageView) findViewById(R.id.remote_d1);
		d1.setOnLongClickListener(this);
		ImageView d2 = (ImageView) findViewById(R.id.remote_d2);
		d2.setOnLongClickListener(this);
		ImageView d3 = (ImageView) findViewById(R.id.remote_d3);
		d3.setOnLongClickListener(this);
		ImageView d4 = (ImageView) findViewById(R.id.remote_d4);
		d4.setOnLongClickListener(this);
		ImageView e1 = (ImageView) findViewById(R.id.remote_e1);
		e1.setOnLongClickListener(this);
		ImageView e2 = (ImageView) findViewById(R.id.remote_e2);
		e2.setOnLongClickListener(this);
		ImageView e3 = (ImageView) findViewById(R.id.remote_e3);
		e3.setOnLongClickListener(this);
		ImageView e4 = (ImageView) findViewById(R.id.remote_e4);
		e4.setOnLongClickListener(this);
		ImageView f1 = (ImageView) findViewById(R.id.remote_f1);
		f1.setOnLongClickListener(this);
		ImageView f2 = (ImageView) findViewById(R.id.remote_f2);
		f2.setOnLongClickListener(this);
		ImageView f3 = (ImageView) findViewById(R.id.remote_f3);
		f3.setOnLongClickListener(this);
		ImageView f4 = (ImageView) findViewById(R.id.remote_f4);
		f4.setOnLongClickListener(this);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
		prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				doScreenUpdate = config.getBoolean("doScreenUpdate", true);
				if (key.equals("doScreenUpdate")) {
					if (!doScreenUpdate) {
						Bitmap initScreen = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(getResources()
								.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/empeg_screen_init",
										null,null))),mAdjustedWidth,mAdjustedHeight,false);
						empegScreen.setImageBitmap(initScreen);
					}
				}
				if (key.equals("refreshRate")) {
					screenRefreshRate = Integer.parseInt(config.getString("refreshRate", "1000"));
				}
				if (key.equals("pixelFont")) {
					getListView().invalidateViews();
				}
				if (key.equals("activeLens")) {
					empegScreen.setColorFilter(getColorFilter());
					lview = getListView();
					lview.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, getColorScheme()));
					lview.setDividerHeight(1);
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
				if (config.getBoolean("doNotifications", true) == false) {
					Intent service = new Intent(getApplicationContext(), NotificationService.class);
					stopService(service);
				} else {
					Intent service = new Intent(getApplicationContext(), NotificationService.class);
					startService(service);
				}
			}
		};
		config.registerOnSharedPreferenceChangeListener(prefListener);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		/*		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);*/
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Button retryComm = (Button) findViewById(R.id.button_refresher);
		retryComm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pScroller.setVisibility(View.VISIBLE);
				pNotFound.setVisibility(View.GONE);
				new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});

		pHistoryButton = (Button) findViewById(R.id.button_pl_up);
		pHistoryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gData.playlistHistory.remove(gData.playlistHistory.size()-1);
				new DownloadDataTask().execute("http://"+playerIP+gData.playlistHistory.get(gData.playlistHistory.size()-1)+"&EXT=.htm","noAdd");
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});
		
		pHomeButton = (Button) findViewById(R.id.button_pl_home);
		pHomeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gData.playlistHistory.clear();
				new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		//Log.i("TABLETMAIN","onResume()");
		playerIP = config.getString("activeEmpegIP", "none");
		//Log.i("TABLETMAIN","saved IP: "+playerIP);

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
		//Log.i("TABLETMAIN","onPause()");
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
		//Log.i("ONACTIVITYRESULT","requestCode="+requestCode+", resultCode="+resultCode+", data="+data);
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ADD_IP_REQUEST_CODE:
			if (!config.getString("activeEmpegIP", "none").equals("none")) {
				if (screenHandler != null) {
					screenHandler.removeCallbacks(sr);
				}
				playerIP = config.getString("activeEmpegIP", "none");

				imageURL = "http://"+playerIP+"/proc/empeg_screen.png";

				// send a hello msg to the empeg
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=NODATA&POPUP%201%20%20Empeg%20Remote%20configured");

				// load/reload the playlist
				new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");

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
		switch(item.getItemId()) {
		case R.id.action_discovery:
			startActivityForResult(new Intent(this,AddEmpeg.class), ADD_IP_REQUEST_CODE);
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, Settings.class));
			return true;
			case R.id.action_send_message:
				// popup text entry
				MaterialDialog dialog = new MaterialDialog.Builder(this)
						.theme(Theme.LIGHT)
						.title("Send Message")
						.customView(R.layout.send_message_popup, true)
						.positiveText("Send")
						.negativeText("Cancel")
						.onPositive(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								new sendCommand().execute("http://" + playerIP +
										"/proc/empeg_notify?button=NODATA&POPUP%20" +
										picker.getValue() + "%20" +
										messageInput.getText().toString().replace(" ", "%20"));
							}
						})
						.build();

				picker = (MaterialNumberPicker) dialog.getCustomView().findViewById(R.id.durationPicker);
				picker.setValue(5);
				positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
				messageInput = (EditText) dialog.getCustomView().findViewById(R.id.message_text);
				messageInput.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						positiveAction.setEnabled(s.toString().trim().length() > 0);
					}

					@Override
					public void afterTextChanged(Editable s) {
					}
				});

				dialog.show();
				positiveAction.setEnabled(false); // disabled by default

				return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key != null) {
			//Log.i("PAGER_ACTIVITY","onSharedPreferenceChanged: key: "+key);
		}
	}

	public class sendCommand extends AsyncTask<String, String, String> {
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
				//Log.i("TABLETMAIN","MalformedURLException - sendCommand");
			} catch (IOException e) {
				//Log.i("TABLETMAIN","IOException - sendCommand");
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

	public void buttonPress(View v) {

		switch (v.getId()) {
		case R.id.remote_a1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=One");
			break;
		case R.id.remote_a2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Two");
			break;
		case R.id.remote_a3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Three");
			break;
		case R.id.remote_a4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Source");
			break;
		case R.id.remote_b1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Four");
			break;
		case R.id.remote_b2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Five");
			break;
		case R.id.remote_b3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Six");
			break;
		case R.id.remote_b4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Tuner");
			break;
		case R.id.remote_c1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Seven");
			break;
		case R.id.remote_c2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Eight");
			break;
		case R.id.remote_c3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Nine");
			break;
		case R.id.remote_c4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=SelectMode");
			break;
		case R.id.remote_d1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
			break;
		case R.id.remote_d2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Zero");
			break;
		case R.id.remote_d3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Search");
			break;
		case R.id.remote_d4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Sound");
			break;
		case R.id.remote_e1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Prev");
			break;
		case R.id.remote_e2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Next");
			break;
		case R.id.remote_e3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Menu");
			break;
		case R.id.remote_e4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=KnobRight");
			break;
		case R.id.remote_f1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Info");
			break;
		case R.id.remote_f2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Visual");
			break;
		case R.id.remote_f3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Play");
			break;
		case R.id.remote_f4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=KnobLeft");
			break;
		case R.id.remote_g1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=HijackMenu");
			break;
		}
		if (doVibrate) {
			vibradora.vibrate(50);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()) {
		case R.id.remote_a1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=One.L");
			break;
		case R.id.remote_a2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Two.L");
			break;
		case R.id.remote_a3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Three.L");
			break;
		case R.id.remote_a4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Source.L");
			break;
		case R.id.remote_b1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Four.L");
			break;
		case R.id.remote_b2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Five.L");
			break;
		case R.id.remote_b3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Six.L");
			break;
		case R.id.remote_b4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Tuner.L");
			break;
		case R.id.remote_c1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Seven.L");
			break;
		case R.id.remote_c2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Eight.L");
			break;
		case R.id.remote_c3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Nine.L");
			break;
		case R.id.remote_c4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=SelectMode.L");
			break;
		case R.id.remote_d1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
			break;
		case R.id.remote_d2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Zero.L");
			break;
		case R.id.remote_d3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Search.L");
			break;
		case R.id.remote_d4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Sound.L");
			break;
		case R.id.remote_e1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Prev.L");
			break;
		case R.id.remote_e2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Next.L");
			break;
		case R.id.remote_e3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Menu.L");
			break;
		case R.id.remote_e4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=VolUp.L");
			break;
		case R.id.remote_f1:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Info.L");
			break;
		case R.id.remote_f2:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Visual.L");
			break;
		case R.id.remote_f3:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Play.L");
			break;
		case R.id.remote_f4:
			new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=VolDown.L");
			break;
		}
		if (doVibrate) {
			vibradora.vibrate(50);
		}
		return true;
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
			return null; // white = no matrix involvement
		default:
			return new ColorMatrixColorFilter(blueMatrix);
		}
	}

	private int[] getColorScheme() {
		activeLens = Integer.parseInt(config.getString("activeLens","0"));
		switch (activeLens) {
		case 0:
			return new int[] {0,0xFF33B5E5,0};
		case 1:
			return new int[] {0,0xFFFF0000,0};
		case 2:
			return new int[] {0,0xFFFFFF00,0};
		case 3:
			return new int[] {0,0xFF00FF00,0};
		case 4:
			return new int[] {0,0xFFFFFFFF,0};
		default:
			return new int[] {0,0xFF33B5E5,0};
		}
	}

	private int getProgressBar() {
		activeLens = Integer.parseInt(config.getString("activeLens","0"));
		switch (activeLens) {
		case 0:
			return R.id.blue_progressbar;
		case 1:
			return R.id.red_progressbar;
		case 2:
			return R.id.yellow_progressbar;
		case 3:
			return R.id.green_progressbar;
		case 4:
			return R.id.white_progressbar;
		default:
			return R.id.blue_progressbar;
		}
	}

	public void playHandler(View v) {
		//Log.i("TABLETMAIN","playhandler() pressed...");
		//get the row the clicked button is in
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		ImageView btnChild = (ImageView)vwParentRow.getChildAt(0);
		//Log.i("TABLETMAIN","playhandler "+btnChild.getTag());
		new sendCommand().execute("http://"+playerIP+btnChild.getTag());
		if (doVibrate) {
			vibradora.vibrate(50);
		}      
	}

	public void plEditHandler(View v) {
		//get the row the clicked button is in
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		ImageView btnChild = (ImageView)vwParentRow.getChildAt(2);
		//Log.i("TABLETMAIN","plEdithandler "+btnChild.getTag());

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
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {

		if (e.getAction() == KeyEvent.ACTION_DOWN) {
			if (e.getKeyCode() == KeyEvent.KEYCODE_A || e.getKeyCode() == KeyEvent.KEYCODE_B || e.getKeyCode() == KeyEvent.KEYCODE_C || e.getKeyCode() == KeyEvent.KEYCODE_2) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Two");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_D || e.getKeyCode() == KeyEvent.KEYCODE_E || e.getKeyCode() == KeyEvent.KEYCODE_F || e.getKeyCode() == KeyEvent.KEYCODE_3) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Three");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_G || e.getKeyCode() == KeyEvent.KEYCODE_H || e.getKeyCode() == KeyEvent.KEYCODE_I || e.getKeyCode() == KeyEvent.KEYCODE_4) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Four");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_J || e.getKeyCode() == KeyEvent.KEYCODE_K || e.getKeyCode() == KeyEvent.KEYCODE_L || e.getKeyCode() == KeyEvent.KEYCODE_5) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Five");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_M || e.getKeyCode() == KeyEvent.KEYCODE_N || e.getKeyCode() == KeyEvent.KEYCODE_O || e.getKeyCode() == KeyEvent.KEYCODE_6) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Six");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_P || e.getKeyCode() == KeyEvent.KEYCODE_R || e.getKeyCode() == KeyEvent.KEYCODE_S || e.getKeyCode() == KeyEvent.KEYCODE_7) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Seven");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_T || e.getKeyCode() == KeyEvent.KEYCODE_U || e.getKeyCode() == KeyEvent.KEYCODE_V || e.getKeyCode() == KeyEvent.KEYCODE_8) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Eight");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_W || e.getKeyCode() == KeyEvent.KEYCODE_X || e.getKeyCode() == KeyEvent.KEYCODE_Y || e.getKeyCode() == KeyEvent.KEYCODE_9) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Nine");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_Q || e.getKeyCode() == KeyEvent.KEYCODE_Z || e.getKeyCode() == KeyEvent.KEYCODE_0) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Zero");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				KeyboardPanel.setClosed();
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Menu");
				return true;
			}
			if (e.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				new sendCommand().execute("http://"+playerIP+"/proc/empeg_notify?button=Cancel");
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
				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				//Log.i("TABLETMAIN","MalformedURLException - DownloadImageTask");
			} catch (IOException e) {
				//Log.i("TABLETMAIN","IOException - DownloadImageTask");
				show404();
			}
			return updatedScreen;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			Bitmap updatedScreen = null;
			if (result != null) {
				updatedScreen = Bitmap.createScaledBitmap(result,mAdjustedWidth,mAdjustedHeight,false);
			} else {
				updatedScreen = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(getResources()
						.openRawResource(getResources().getIdentifier("com.chasinglemons.empeg:drawable/not_found",null,null))),
						mAdjustedWidth,mAdjustedHeight,false);
			}
			empegScreen.setImageBitmap(updatedScreen);
		}
	}

	public class DownloadDataTask extends AsyncTask<String, String, ArrayList<String>> {

		ArrayList<String> result = new ArrayList<String>();

		@Override
		protected void onPreExecute() {
			progresso = (ProgressBar) findViewById(getProgressBar());
			progresso.setVisibility(View.VISIBLE);
		};

		@Override
		protected ArrayList<String> doInBackground(String... url) {

			// progressor.setVisibility(View.VISIBLE);

			try {
				HttpClient httpclient = new DefaultHttpClient();
				// //Log.i("EMPEG","FETCHING: "+url[0]);
				HttpGet httpget = new HttpGet(url[0]);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				result.add(httpclient.execute(httpget, responseHandler));

				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				//Log.i("PLAYLISTEXPLORER","DownloadDataTask MalformedURLException: "+e);
			} catch (IOException e) {
				//Log.i("PLAYLISTEXPLORER","DownloadDataTask IOException: "+e);
				// empeg not found
				show404();
			}

			result.add(url[1]); //to add to history or to not add to history...

			return result;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			//Log.i("EMPEG","onPostExecute result.get(0): "+result.get(0));
			if (result.get(0) != null) {
				// parse the result
				List<Playlist> pList = new ArrayList<Playlist>();
				List<String> pListElements = new ArrayList<String>();
				List<String> pListLinks = new ArrayList<String>();
				String pName = "";
				String pLength = "";
				String pType = "";
				String pArtist = "";
				String pSource = "";
				Document doc = Jsoup.parse(result.get(0));
				Elements trs = doc.getElementsByTag("tr");
				for (Element tr : trs) {
					Elements tds = tr.getElementsByTag("td");

					for (Element td : tds) {
						// //Log.i("EMPEG_TD","sibindex: "+td.elementSiblingIndex());
						/*if (td.elementSiblingIndex() > 5) {
						pList.add(td.text());
					} else {
						pList.add(td.attr("href"));
					}*/
						// //Log.i("EMPEG_TD",""+td.html());

						Element link = td.select("a").first();
						try {
							// //Log.i("EMPEG_TD","linkHref: "+link.attr("href"));
							pListLinks.add(link.attr("href"));
						} catch (NullPointerException e) {

						}

						if (td.elementSiblingIndex() == 5) {
							// //Log.i("EMPEG_TD","NAME: "+td.text());
							pName = td.text();
						}
						if (td.elementSiblingIndex() == 6) {
							// //Log.i("EMPEG_TD","LENGTH: "+td.text());
							pLength = td.text();
						}
						if (td.elementSiblingIndex() == 7) {
							// //Log.i("EMPEG_TD","TYPE: "+td.text());
							pType = td.text();
						}
						if (td.elementSiblingIndex() == 8) {
							// //Log.i("EMPEG_TD","ARTIST: "+td.text());
							pArtist = td.text();
						}
						if (td.elementSiblingIndex() == 9) {
							// //Log.i("EMPEG_TD","SOURCE: "+td.text());
							pSource = td.text();
						}
					}

					// build the INSERT link
					String myInsert = pListLinks.get(2).replace("-","!");

					// String pName, String pStreamURL, String pPlayURL, String pInsertURL, String pEnqueueURL, String pAppendURL, String pURL, String pLength, String pType, String pArtist, String pSource
					if (pListLinks.size() > 5) {
						//Log.e("EMPEG_TD","pLiskLinks "+pListLinks.toString());
						//					//Log.e("EMPEG_TD","pLiskLinks.get(5) "+pListLinks.get(5));
						if (pListLinks.get(5).endsWith(".mp3")) { // this is a song, not a dir
							//						//Log.e("EMPEG_TD","I am the end of the list!!!!");
							pList.add(new Playlist(pName,pListLinks.get(0),pListLinks.get(1),myInsert,pListLinks.get(2),pListLinks.get(3),"none",pLength,pType,pArtist,pSource));
						} else {
							pList.add(new Playlist(pName,pListLinks.get(0),pListLinks.get(1),myInsert,pListLinks.get(2),pListLinks.get(3),pListLinks.get(5),pLength,pType,pArtist,pSource));
						}
					} else {
						// //Log.e("EMPEG_TD","HEADLIST");
						pList.add(new Playlist("000000"+pName,pListLinks.get(0),pListLinks.get(1),myInsert,pListLinks.get(2),pListLinks.get(4),"head",pLength,pType,pArtist,pSource));
						// //Log.i("PLAYLIST_EXPLORER","pListLinks.get(4) = "+pListLinks.get(4));

						if (result.get(1).equals("add")) {
							gData.playlistHistory.add(pListLinks.get(4)); // write this URL to playlist history
						}
						if (!pName.equals("All Music") && pListLinks.size() > 1 /* if nothing is returned (player off or not configured)*/) {
							pHistoryLayout.setVisibility(View.VISIBLE);
						} else {
							pHistoryLayout.setVisibility(View.GONE);
						}
					}
					pListElements.clear();
					pListLinks.clear();
				}

				// Sort the list
				/*				Collections.sort(pList, new Comparator<Playlist>() {               
					@Override
					public int compare(Playlist p1, Playlist p2) {
						return p1.getpName().compareTo(p2.getpName());
					}
				});*/

				setListAdapter(new PlaylistAdapter(TabletMain.this, pList));
				lview = getListView();
				lview.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, getColorScheme()));
				lview.setDividerHeight(1);
			}
			progresso.setVisibility(View.GONE);
		}
	}

	public void playlistHandler(View v) {
		//get the row the clicked button is in
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		Button btnChild = (Button)vwParentRow.getChildAt(0);
		// //Log.i("EMPEG","playlisthandler "+btnChild.getTag());
		new DownloadDataTask().execute("http://"+playerIP+btnChild.getTag());
		// vwParentRow.refreshDrawableState();       
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (position > 0) {
			Playlist item = (Playlist) getListAdapter().getItem(position);
			//			 //Log.i("PLAYLISTEXPL","onListItemClick item.getpURL() = "+item.getpURL());
			if (!item.getpURL().equals("none")) {
				new DownloadDataTask().execute("http://"+playerIP+item.getpURL(),"add");
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		}
	}

	private void show404() {
		errorHandler = new Handler(Looper.getMainLooper());
		errorHandler.post(new Runnable(){

			@Override
			public void run() {

				pScroller.setVisibility(View.GONE);
				pHistoryLayout.setVisibility(View.GONE);
				progresso.setVisibility(View.GONE);
				pNotFound.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void onPanelClosed(KeyboardPanel panel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPanelOpened(KeyboardPanel panel) {
		// TODO Auto-generated method stub
		
	}
}
