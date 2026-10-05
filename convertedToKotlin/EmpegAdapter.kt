package com.chasinglemons.empeg

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EmpegAdapter(private val activity: Activity, objects: List<*>) :
    ArrayAdapter<Any?>(activity, R.layout.my_two_lines, objects) {
    private val playlistitems: List<Playlist>

    init {
        this.playlistitems = objects
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        var plView: PlaylistView? = null

        if (rowView == null) {
            // Get a new instance of the row layout view
            val inflater = activity.layoutInflater
            rowView = inflater.inflate(R.layout.my_two_lines, null)

            // Hold the view objects in an object,
            // so they don't need to be re-fetched
            plView = PlaylistView()
            plView.plLLayout = rowView.findViewById<View>(R.id.pl_llayout) as LinearLayout
            plView.plName = rowView.findViewById<View>(R.id.pl_name) as TextView

            //			plView.plLength = (TextView) rowView.findViewById(R.id.pl_length);

//			plView.plStream = (Button) rowView.findViewById(R.id.stream_button);
            plView.plPlay = rowView.findViewById<View>(R.id.play_button) as Button

            //			plView.plInsert = (Button) rowView.findViewById(R.id.insert_button);
//			plView.plAppend = (Button) rowView.findViewById(R.id.append_button);

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
            plView.plName!!.textSize = 22f
            //			plView.plLength.setVisibility(View.GONE);
        } else {
            plView!!.plLLayout!!.setBackgroundColor(Color.BLACK)
            plView.plName!!.text = currentpl.getpName()
            plView.plName!!.setTextColor(Color.WHITE)
            //			plView.plLength.setText(currentpl.getpLength());
            plView.plName!!.textSize = 18f
            //			plView.plLength.setVisibility(View.VISIBLE);
        }


        /*		
		if (revplaylistitemsePercentage > 11){
			plView.erState.setTextColor(Color.RED);
		} else if (revplaylistitemsePercentage > 6 && revplaylistitemsePercentage <= 10){
			plView.erState.setTextColor(Color.YELLOW);
		} else {
			plView.erState.setTextColor(Color.GREEN);
		}*/

        //Log.i("","setting getStreamURL: "+currentpl.getpStreamURL());
        //		plView.plStream.setOnClickListener(this);
//		plView.plStream.setTag(currentpl.getpStreamURL());

        //Log.i("","setting getPlayURL: "+currentpl.getpPlayURL());
        //		plView.plPlay.setOnClickListener(this);
        plView.plPlay!!.tag = currentpl.getpPlayURL()

        //Log.i("","setting getInsertURL: "+currentpl.getpInsertURL());
        //		plView.plInsert.setOnClickListener(this);
        plView.plInsert!!.tag = currentpl.getpInsertURL()

        //Log.i("","setting getAppendURL: "+currentpl.getpAppendURL());
        //		plView.plAppend.setOnClickListener(this);
        plView.plAppend!!.tag = currentpl.getpAppendURL()

        return rowView!!
    }

    protected class PlaylistView {
        var plLLayout: LinearLayout? = null
        var plName: TextView? = null

        //		protected TextView plLength;
        //		protected Button plStream;
        var plPlay: Button? = null
        var plInsert: Button? = null
        var plAppend: Button? = null
    } /*	public void onClick(View v) {
//			//Log.i("DIA","YOU CLICKED@@@@@@@ "+v.getId());
switch (v.getId()) {
case R.id.stream_button:
	//Log.i("","STREAM: "+v.getTag());
	break;
case R.id.play_button:
	//Log.i("","PLAY: "+v.getTag());
	break;
case R.id.insert_button:
	//Log.i("","INSERT: "+v.getTag());
	break;
case R.id.append_button:
	//Log.i("","APPEND: "+v.getTag());
	break; 
}
	}*/
}