package com.chasinglemons.empeg;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

public class PlaylistExplorer extends ListFragment {

	ProgressBar progresso;
	SharedPreferences config;
	String playerIP;
	Activity activity;
	GlobalData gData;
	Button pHistoryButton,pHomeButton;
	LinearLayout pHistoryLayout,pScroller,pNotFound;
	boolean doVibrate;
	Vibrator vibradora;
	private OnSharedPreferenceChangeListener prefListener;
	Handler errorHandler;
	int activeLens = 0;
	View thisView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.playlist_explorer, container, false);

		activity = getActivity();
		thisView = view;

		gData = ((GlobalData)activity.getApplicationContext());

		config = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
		prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				if (key.equals("pixelFont")) {
					getListView().invalidateViews();
				}
				if (key.equals("activeLens")) {
					getListView().setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, getColorScheme()));
					getListView().setDividerHeight(1);
					getListView().invalidateViews();
				}
			}
		};
		config.registerOnSharedPreferenceChangeListener(prefListener);

		LocalBroadcastManager.getInstance(activity).registerReceiver(mIPChange,
				new IntentFilter("ip-change"));

		vibradora = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
		doVibrate = config.getBoolean("doVibrate", true);

		pNotFound = (LinearLayout) view.findViewById(R.id.List_Not_Found);
		pScroller = (LinearLayout) view.findViewById(R.id.List_Scroller);
		pHistoryLayout = (LinearLayout) view.findViewById(R.id.List_Back);

		if (!config.getString("activeEmpegIP", "none").equals("none")) {
			playerIP = config.getString("activeEmpegIP", "none");
			new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");
		}

		Button retryComm = (Button) view.findViewById(R.id.button_refresher);
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

		pHistoryButton = (Button) view.findViewById(R.id.button_pl_up);
		pHistoryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// //Log.i("PAGER_ACTIVITY","gData.getPlaylistHistory().size() = "+gData.playlistHistory.size());
				// //Log.i("PAGER_ACTIVITY","gData.playlistHistory contains "+gData.playlistHistory.toString());
				// //Log.i("PAGER_ACTIVITY","we want to now remove the one on the end: ("+gData.playlistHistory.get(gData.playlistHistory.size()-1)+")");
				gData.playlistHistory.remove(gData.playlistHistory.size()-1);
				// //Log.i("PAGER_ACTIVITY","attempting to load: "+"http://"+playerIP+gData.playlistHistory.get(gData.playlistHistory.size()-1));
				new DownloadDataTask().execute("http://"+playerIP+gData.playlistHistory.get(gData.playlistHistory.size()-1)+"&EXT=.htm","noAdd");
				if (doVibrate) {
					vibradora.vibrate(50);
				}
			}
		});
		
		pHomeButton = (Button) view.findViewById(R.id.button_pl_home);
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

		return view;
	}

	public class DownloadDataTask extends AsyncTask<String, String, ArrayList<String>> {

		ArrayList<String> result = new ArrayList<String>();

		@Override
		protected void onPreExecute() {
			progresso = (ProgressBar) thisView.findViewById(getProgressBar());
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
						//	Log.i("EMPEG_TD","linkHref: "+link.attr("href"));
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


						/* Elements links = td.select("a");
					for (Element link : links) {
						pListElements.add(link.attr("href"));
						pListElements.add(link.text());
					}*/
						// headList = (td.elementSiblingIndex() > 5 ? false : true);
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
						// Log.e("EMPEG_TD","HEADLIST");
						pList.add(new Playlist("000000"+pName,pListLinks.get(0),pListLinks.get(1),myInsert,pListLinks.get(2),pListLinks.get(4),"head",pLength,pType,pArtist,pSource));
						// Log.i("PLAYLIST_EXPLORER","pListLinks.get(4) = "+pListLinks.get(4));

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

				setListAdapter(new PlaylistAdapter(activity, pList));
				if (getListView() != null) {
					getListView().setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, getColorScheme()));
					getListView().setDividerHeight(1);
				}
			}
			progresso.setVisibility(View.GONE);
		}
	}

	public class sendCommand extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... url) {
			String responseBody = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				// //Log.i("EMPEG","FETCHING: "+url[0]);
				HttpGet httpget = new HttpGet(url[0]);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				responseBody = httpclient.execute(httpget, responseHandler);

				httpclient.getConnectionManager().shutdown();
			} catch (MalformedURLException e) {
				//Log.i("PLAYLISTEXPLORER","SEND_CMD MalformedURLException: "+e);
			} catch (IOException e) {
				//Log.i("PLAYLISTEXPLORER","SEND_CMD IOException: "+e);
			}
			return responseBody;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
		}

		@Override
		protected void onPostExecute(String result) {
			// //Log.i("EMPEG","onPostExecute: "+result);
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

	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mIPChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get extra data included in the Intent
			//			//Log.d("PLAYLISTEXPLORER", "Got newIP");
			playerIP = intent.getStringExtra("newIP");
			new DownloadDataTask().execute("http://"+playerIP+"/?FID=101&EXT=.htm","add");
		}
	};

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
}