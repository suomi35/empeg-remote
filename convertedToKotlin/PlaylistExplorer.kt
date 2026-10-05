package com.chasinglemons.empeg

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.fragment.app.ListFragment
import org.apache.http.client.HttpClient
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException

class PlaylistExplorer : ListFragment() {
    var progresso: ProgressBar? = null
    var config: SharedPreferences? = null
    var playerIP: String? = null
    var activity: Activity? = null
    var gData: GlobalData? = null
    var pHistoryButton: Button? = null
    var pHomeButton: Button? = null
    var pHistoryLayout: LinearLayout? = null
    var pScroller: LinearLayout? = null
    var pNotFound: LinearLayout? = null
    var doVibrate: Boolean = false
    var vibradora: Vibrator? = null
    private var prefListener: OnSharedPreferenceChangeListener? = null
    var errorHandler: Handler? = null
    var activeLens: Int = 0
    var thisView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.playlist_explorer, container, false)

        activity = getActivity()
        thisView = view

        gData = (activity!!.applicationContext as GlobalData)

        config = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        prefListener = object : OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
                if (key == "pixelFont") {
                    listView.invalidateViews()
                }
                if (key == "activeLens") {
                    listView.divider = GradientDrawable(
                        GradientDrawable.Orientation.RIGHT_LEFT,
                        this.colorScheme
                    )
                    listView.dividerHeight = 1
                    listView.invalidateViews()
                }
            }
        }
        config.registerOnSharedPreferenceChangeListener(prefListener)

        LocalBroadcastManager.getInstance(activity).registerReceiver(
            mIPChange,
            IntentFilter("ip-change")
        )

        vibradora = activity!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        doVibrate = config.getBoolean("doVibrate", true)

        pNotFound = view.findViewById<View>(R.id.List_Not_Found) as LinearLayout
        pScroller = view.findViewById<View>(R.id.List_Scroller) as LinearLayout
        pHistoryLayout = view.findViewById<View>(R.id.List_Back) as LinearLayout

        if (config.getString("activeEmpegIP", "none") != "none") {
            playerIP = config.getString("activeEmpegIP", "none")
            DownloadDataTask().execute("http://$playerIP/?FID=101&EXT=.htm", "add")
        }

        val retryComm = view.findViewById<View>(R.id.button_refresher) as Button
        retryComm.setOnClickListener {
            pScroller.setVisibility(View.VISIBLE)
            pNotFound.setVisibility(View.GONE)
            DownloadDataTask()
                .execute("http://$playerIP/?FID=101&EXT=.htm", "add")
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }

        pHistoryButton = view.findViewById<View>(R.id.button_pl_up) as Button
        pHistoryButton!!.setOnClickListener { // //Log.i("PAGER_ACTIVITY","gData.getPlaylistHistory().size() = "+gData.playlistHistory.size());
            // //Log.i("PAGER_ACTIVITY","gData.playlistHistory contains "+gData.playlistHistory.toString());
            // //Log.i("PAGER_ACTIVITY","we want to now remove the one on the end: ("+gData.playlistHistory.get(gData.playlistHistory.size()-1)+")");
            gData!!.playlistHistory.removeAt(gData!!.playlistHistory.size - 1)
            // //Log.i("PAGER_ACTIVITY","attempting to load: "+"http://"+playerIP+gData.playlistHistory.get(gData.playlistHistory.size()-1));
            DownloadDataTask().execute(
                "http://$playerIP" + gData!!.playlistHistory[gData!!.playlistHistory.size - 1] + "&EXT=.htm",
                "noAdd"
            )
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }

        pHomeButton = view.findViewById<View>(R.id.button_pl_home) as Button
        pHomeButton!!.setOnClickListener {
            gData!!.playlistHistory.clear()
            DownloadDataTask()
                .execute("http://$playerIP/?FID=101&EXT=.htm", "add")
            if (doVibrate) {
                vibradora.vibrate(50)
            }
        }

        return view
    }

    inner class DownloadDataTask :
        AsyncTask<String?, String?, ArrayList<String?>?>() {
        var result: ArrayList<String> = ArrayList()

        override fun onPreExecute() {
            progresso = thisView!!.findViewById<View>(this.progressBar) as ProgressBar
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
                            //	Log.i("EMPEG_TD","linkHref: "+link.attr("href"));
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


                        /* Elements links = td.select("a");
					for (Element link : links) {
						pListElements.add(link.attr("href"));
						pListElements.add(link.text());
					}*/
                        // headList = (td.elementSiblingIndex() > 5 ? false : true);
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
                        // Log.e("EMPEG_TD","HEADLIST");
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

                        // Log.i("PLAYLIST_EXPLORER","pListLinks.get(4) = "+pListLinks.get(4));
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
                listAdapter = PlaylistAdapter(activity!!, pList)
                if (listView != null) {
                    listView.divider = GradientDrawable(
                        GradientDrawable.Orientation.RIGHT_LEFT,
                        this.colorScheme
                    )
                    listView.dividerHeight = 1
                }
            }
            progresso!!.visibility = View.GONE
        }
    }

    inner class sendCommand : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg url: String): String {
            var responseBody = ""
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                // //Log.i("EMPEG","FETCHING: "+url[0]);
                val httpget: HttpGet = HttpGet(url[0])
                val responseHandler: ResponseHandler<String> = BasicResponseHandler()
                responseBody = httpclient.execute(httpget, responseHandler)

                httpclient.getConnectionManager().shutdown()
            } catch (e: MalformedURLException) {
                //Log.i("PLAYLISTEXPLORER","SEND_CMD MalformedURLException: "+e);
            } catch (e: IOException) {
                //Log.i("PLAYLISTEXPLORER","SEND_CMD IOException: "+e);
            }
            return responseBody
        }

        override fun onProgressUpdate(vararg progress: String) {
        }

        override fun onPostExecute(result: String) {
            // //Log.i("EMPEG","onPostExecute: "+result);
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
            val item = listAdapter!!.getItem(position) as Playlist
            //			 //Log.i("PLAYLISTEXPL","onListItemClick item.getpURL() = "+item.getpURL());
            if (item.getpURL() != "none") {
                DownloadDataTask().execute("http://" + playerIP + item.getpURL(), "add")
                if (doVibrate) {
                    vibradora.vibrate(50)
                }
            }
        }
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private val mIPChange: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            //			//Log.d("PLAYLISTEXPLORER", "Got newIP");
            playerIP = intent.getStringExtra("newIP")
            DownloadDataTask().execute("http://$playerIP/?FID=101&EXT=.htm", "add")
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

    private fun show404() {
        errorHandler = Handler(Looper.getMainLooper())
        errorHandler!!.post {
            pScroller.setVisibility(View.GONE)
            pHistoryLayout.setVisibility(View.GONE)
            progresso!!.visibility = View.GONE
            pNotFound.setVisibility(View.VISIBLE)
        }
    }
}