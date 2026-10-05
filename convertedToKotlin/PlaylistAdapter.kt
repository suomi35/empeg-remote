package com.chasinglemons.empeg

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class PlaylistAdapter(private val activity: Activity, objects: List<*>) :
    ArrayAdapter<Any?>(activity, R.layout.playlist_row, objects) {
    private val playlistitems: List<Playlist>

    init {
        this.playlistitems = objects
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        var plView: PlaylistView? = null
        val pixelfont = Typeface.createFromAsset(context.assets, "fonts/pixelmix.ttf")
        val pixelfontBold = Typeface.createFromAsset(context.assets, "fonts/pixelmix_bold.ttf")
        val config = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)

        if (rowView == null) {
            // Get a new instance of the row layout view
            val inflater = activity.layoutInflater
            rowView = inflater.inflate(R.layout.playlist_row, null)

            // Hold the view objects in an object,
            // so they don't need to be re-fetched
            plView = PlaylistView()
            plView.plLLayout = rowView.findViewById<View>(R.id.pl_llayout) as LinearLayout
            plView.plName = rowView.findViewById<View>(R.id.pl_name) as TextView
            plView.plPlay = rowView.findViewById<View>(R.id.play_button) as ImageView
            plView.plMore = rowView.findViewById<View>(R.id.more_button) as ImageView

            // Cache the view objects in the tag,
            // so they can be re-accessed later
            rowView.tag = plView
        } else {
            plView = rowView.tag as PlaylistView
        }

        // Transfer the stock data from the data object
        // to the view objects
        val currentpl = playlistitems[position]
        if (currentpl.getpName().startsWith("000000")) {
            plView!!.plLLayout!!.setBackgroundColor(0x22FFFFFF)
            plView.plName!!.setTextColor(Color.RED)
            plView.plName!!.text = currentpl.getpName().substring(6)
            plView.plName!!.textSize = 24f
            if (config.getBoolean("pixelFont", false)) {
                plView.plName!!.typeface = pixelfontBold
            } else {
                plView.plName!!.typeface = null
            }

            //			plView.plLength.setVisibility(View.GONE);
        } else {
            plView!!.plLLayout!!.setBackgroundColor(Color.BLACK)
            plView.plName!!.text = currentpl.getpName()
            plView.plName!!.setTextColor(Color.WHITE)
            //			plView.plLength.setText("("+currentpl.getpLength()+" items)");
            plView.plName!!.textSize = 18f
            if (config.getBoolean("pixelFont", false)) {
                plView.plName!!.typeface = pixelfont
            } else {
                plView.plName!!.typeface = null
            }
        }

        //Log.i("","setting getPlayURL: "+currentpl.getpPlayURL());
        //		plView.plPlay.setOnClickListener(this);
        plView.plPlay!!.tag = currentpl.getpPlayURL()


        // stack up the URLs as - "enqueue:append:insert"
        plView.plMore!!.tag =
            currentpl.getpEnqueueURL() + ":" + currentpl.getpAppendURL() + ":" + currentpl.getpInsertURL() + ":" + currentpl.getpStreamURL()

        return rowView!!
    }

    protected class PlaylistView {
        var plLLayout: LinearLayout? = null
        var plName: TextView? = null
        var plPlay: ImageView? = null
        var plMore: ImageView? = null
    }
}