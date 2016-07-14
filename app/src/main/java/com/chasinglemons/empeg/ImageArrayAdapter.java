package com.chasinglemons.empeg;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;


public class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
	private int index = 0;
	private int[] resourceIds = null;

	public ImageArrayAdapter(Context context, int textViewResourceId,
			CharSequence[] objects, int[] ids, int i, String isPro) {
		super(context, textViewResourceId, objects);

		index = i;
		resourceIds = ids;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
		View row = inflater.inflate(R.layout.listitem, parent, false);

		ImageView imageView = (ImageView)row.findViewById(R.id.image);
		imageView.setImageResource(resourceIds[position]);

		CheckedTextView checkedTextView = (CheckedTextView)row.findViewById(
			R.id.check);

		checkedTextView.setText(getItem(position));

			if (position == index) {
				checkedTextView.setChecked(true);
			}

		return row;
	}
}
