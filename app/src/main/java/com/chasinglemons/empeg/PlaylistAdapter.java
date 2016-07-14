package com.chasinglemons.empeg;

import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unchecked")
public class PlaylistAdapter extends ArrayAdapter<Object> {
	private final Activity activity;
	private final List<Playlist> playlistitems;

	public PlaylistAdapter(Activity activity, List objects) {
		super(activity, R.layout.playlist_row, objects);
		this.activity = activity;
		this.playlistitems = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		PlaylistView plView = null;
		Typeface pixelfont = Typeface.createFromAsset(getContext().getAssets(),"fonts/pixelmix.ttf");
		Typeface pixelfontBold = Typeface.createFromAsset(getContext().getAssets(),"fonts/pixelmix_bold.ttf");
		SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()); 

		if(rowView == null) {
			// Get a new instance of the row layout view
			LayoutInflater inflater = activity.getLayoutInflater();
			rowView = inflater.inflate(R.layout.playlist_row, null);

			// Hold the view objects in an object,
			// so they don't need to be re-fetched
			plView = new PlaylistView();
			plView.plLLayout = (LinearLayout) rowView.findViewById(R.id.pl_llayout);
			plView.plName = (TextView) rowView.findViewById(R.id.pl_name);
			plView.plPlay = (ImageView) rowView.findViewById(R.id.play_button);
			plView.plMore = (ImageView) rowView.findViewById(R.id.more_button);

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
			plView.plName.setTextSize(24);
			if (config.getBoolean("pixelFont", false)) {
				plView.plName.setTypeface(pixelfontBold);
			} else {
				plView.plName.setTypeface(null);
			}
			//			plView.plLength.setVisibility(View.GONE);

		} else {
			plView.plLLayout.setBackgroundColor(Color.BLACK);
			plView.plName.setText(currentpl.getpName());
			plView.plName.setTextColor(Color.WHITE);
			//			plView.plLength.setText("("+currentpl.getpLength()+" items)");
			plView.plName.setTextSize(18);
			if (config.getBoolean("pixelFont", false)) {
				plView.plName.setTypeface(pixelfont);
			} else {
				plView.plName.setTypeface(null);
			}
		}

		//Log.i("","setting getPlayURL: "+currentpl.getpPlayURL());
		//		plView.plPlay.setOnClickListener(this);
		plView.plPlay.setTag(currentpl.getpPlayURL());
		
		// stack up the URLs as - "enqueue:append:insert"
		plView.plMore.setTag(currentpl.getpEnqueueURL()+":"+currentpl.getpAppendURL()+":"+currentpl.getpInsertURL()+":"+currentpl.getpStreamURL());
		
		return rowView;
	}

	protected static class PlaylistView {
		protected LinearLayout plLLayout;
		protected TextView plName;
		protected ImageView plPlay;
		protected ImageView plMore;
	}
}