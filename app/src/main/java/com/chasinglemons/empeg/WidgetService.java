package com.chasinglemons.empeg;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetService extends Service {

	public static final String UPDATE = "update";
	public static final String PAUSE = "pause";
	public static final String PREV = "prev";
	public static final String PLAY = "play";
	public static final String NEXT = "next";
	String empegCounter = "35";
	String lastCounter = "0";
	String thirdCounter = "00";
	String topDisplay,bottomDisplay;
	SharedPreferences config;
	boolean runOnce = false;
	boolean isPaused = false;
	String playerIP;
	RemoteViews remoteView;
	int whichWidget;

	@Override
	public void onStart(Intent intent, int startId) {
		String command = intent.getAction();
		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);

		config = PreferenceManager.getDefaultSharedPreferences(this);
		playerIP = config.getString("activeEmpegIP", "none");
		whichWidget = config.getInt("widget", 0);
		switch (whichWidget) {
		case 0:
			remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
			break;
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

		// prev button pressed
		if(command.equals(PREV)) {
			Log.i("WIDGET SERVICE","PREV");
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Left");
			// play button pressed
		} else if(command.equals(PLAY)) {
			Log.i("WIDGET SERVICE","PLAY");
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Top");
			// next button pressed
		} else if(command.equals(NEXT)) {
			Log.i("WIDGET SERVICE","NEXT");
			new sendButton().execute("http://"+playerIP+"/proc/empeg_notify?button=Right");
			// pause widget
		} else if(command.equals(PAUSE)) {
			if (isPaused) {
				Widget.setAlarm(getApplicationContext(), appWidgetId, 1000);
				remoteView.setInt(R.id.pause_button, "setBackgroundResource", R.drawable.pause_on);
				isPaused = false;
			} else {
				Widget.setAlarm(getApplicationContext(), appWidgetId, -1);
				remoteView.setInt(R.id.pause_button, "setBackgroundResource", R.drawable.pause_off);
				isPaused = true;
			}
			// update
		} else if(command.equals(UPDATE)) {
			Log.i("WIDGET SERVICE","UPDATE");
			if (config.getString("activeEmpegIP", "none").equals("none")) {
				if (!runOnce) {
					Intent noIP = new Intent(this, AddEmpeg.class);
					noIP.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(noIP);
					runOnce = true;
				}
			} else {
				playerIP = config.getString("activeEmpegIP", "none");
				Log.i("WIDGET SERVICE","("+empegCounter+", "+lastCounter+", "+thirdCounter+")");
				new DownloadDataTask().execute("http://"+playerIP+"/proc/empeg_notify");
			}
		}

		switch (whichWidget) {
		case 0:
			// put the text into the textView
			remoteView.setTextViewText(R.id.top_line, topDisplay);
			remoteView.setTextViewText(R.id.bottom_line, bottomDisplay);
			// set buttons
			remoteView.setOnClickPendingIntent(R.id.pause_button,Widget.makeControlPendingIntent(getApplicationContext(),PAUSE,appWidgetId));
			remoteView.setOnClickPendingIntent(R.id.prev_button,Widget.makeControlPendingIntent(getApplicationContext(),PREV,appWidgetId));
			remoteView.setOnClickPendingIntent(R.id.play_button,Widget.makeControlPendingIntent(getApplicationContext(),PLAY,appWidgetId));
			remoteView.setOnClickPendingIntent(R.id.next_button,Widget.makeControlPendingIntent(getApplicationContext(),NEXT,appWidgetId));
			break;
		}
		// apply changes to widget
		appWidgetManager.updateAppWidget(appWidgetId, remoteView);
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class DownloadDataTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... url) {
			String responseBody = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				Log.i("EMPEG","FETCHING: "+url[0]);
				HttpGet httpget = new HttpGet(url[0]);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				responseBody = httpclient.execute(httpget, responseHandler);

				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				Log.i("EMPEG","MalformedURLException");
			} catch (IOException e) {
				Log.i("EMPEG","IOException");
			}
			return responseBody;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
		}

		@Override
		protected void onPostExecute(String result) {
			//			Log.i("EMPEG","onPostExecute: "+result);

			// parse the result
			result = result.replaceAll("(\\r|\\n)", "");
			//			Log.i("","result = "+result);
			Pattern VERSE_PATTERN = Pattern.compile("notify_FidTime = \"(.*?)\";notify_Artist = \"(.*?)\";notify_FID = \"(.*?)\";notify_Genre = \"(.*?)\";notify_MixerInput = \"(.*?)\";notify_Track = \"(.*?)\";notify_Sound = \"(.*?)\";notify_Title = \"(.*?)\";notify_Volume = \"(.*?)\";");
			Matcher m = VERSE_PATTERN.matcher(result);
			while (m.find()) {

				topDisplay = m.group(8);
				bottomDisplay = m.group(2)+" - "+m.group(1).split("  ")[1];
				empegCounter = m.group(1).split("  ")[1];
				Log.i("WIDGET_SERVICE","empegCounter = "+empegCounter);
			}
		}
	}

	public class sendButton extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... url) {
			String responseBody = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				//				Log.i("EMPEG","FETCHING: "+url[0]);
				HttpGet httpget = new HttpGet(url[0]);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				responseBody = httpclient.execute(httpget, responseHandler);

				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				Log.i("EMPEG","MalformedURLException");
			} catch (IOException e) {
				Log.i("EMPEG","IOException");
			}
			return responseBody;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
		}

		@Override
		protected void onPostExecute(String result) {
			//			Log.i("EMPEG","onPostExecute: "+result);
		}
	}
}
