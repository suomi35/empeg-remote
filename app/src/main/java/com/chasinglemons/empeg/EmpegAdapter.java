package com.chasinglemons.empeg;

import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unchecked")
public class EmpegAdapter extends ArrayAdapter<Object> {
	private final Activity activity;
	private final List<Playlist> playlistitems;

	public EmpegAdapter(Activity activity, List objects) {
		super(activity, R.layout.my_two_lines, objects);
		this.activity = activity;
		this.playlistitems = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		PlaylistView plView = null;

		if(rowView == null) {
			// Get a new instance of the row layout view
			LayoutInflater inflater = activity.getLayoutInflater();
			rowView = inflater.inflate(R.layout.my_two_lines, null);

			// Hold the view objects in an object,
			// so they don't need to be re-fetched
			plView = new PlaylistView();
			plView.plLLayout = (LinearLayout) rowView.findViewById(R.id.pl_llayout);
			plView.plName = (TextView) rowView.findViewById(R.id.pl_name);
//			plView.plLength = (TextView) rowView.findViewById(R.id.pl_length);

//			plView.plStream = (Button) rowView.findViewById(R.id.stream_button);
			plView.plPlay = (Button) rowView.findViewById(R.id.play_button);
//			plView.plInsert = (Button) rowView.findViewById(R.id.insert_button);
//			plView.plAppend = (Button) rowView.findViewById(R.id.append_button);

			// Cache the view objects in the tag,
			// so they can be re-accessed later
			rowView.setTag(plView);
		} else {
			plView = (PlaylistView) rowView.getTag();
		}

		// Transfer the stock data from the data object
		// to the view objects
		Playlist currentpl = playlistitems.get(position);
		if (currentpl.getpName().startsWith("000000")) {
			plView.plLLayout.setBackgroundColor(0x22FFFFFF);
			plView.plName.setTextColor(Color.RED);
			plView.plName.setText(currentpl.getpName().substring(6));
			plView.plName.setTextSize(22);
//			plView.plLength.setVisibility(View.GONE);
		} else {
			plView.plLLayout.setBackgroundColor(Color.BLACK);
			plView.plName.setText(currentpl.getpName());
			plView.plName.setTextColor(Color.WHITE);
//			plView.plLength.setText(currentpl.getpLength());
			plView.plName.setTextSize(18);
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
		plView.plPlay.setTag(currentpl.getpPlayURL());

		//Log.i("","setting getInsertURL: "+currentpl.getpInsertURL());
		//		plView.plInsert.setOnClickListener(this);
		plView.plInsert.setTag(currentpl.getpInsertURL());

		//Log.i("","setting getAppendURL: "+currentpl.getpAppendURL());
		//		plView.plAppend.setOnClickListener(this);
		plView.plAppend.setTag(currentpl.getpAppendURL());

		return rowView;
	}

	protected static class PlaylistView {
		protected LinearLayout plLLayout;
		protected TextView plName;
//		protected TextView plLength;
//		protected Button plStream;
		protected Button plPlay;
		protected Button plInsert;
		protected Button plAppend;
	}

	/*	public void onClick(View v) {
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