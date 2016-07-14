package com.chasinglemons.empeg;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
	
	private static final String androidns="http://schemas.android.com/apk/res/android";
	private static final String chasinglemonsseekns="http://com.chasinglemons.empeg";
	private static final int DEFAULT_VALUE = 50;
	
	private int mMaxValue = 100;
	private int mMinValue = 0;
	private int mInterval = 1;
	private int mCurrentValue;
	private String mUnits = "sec";
	
	private TextView mStatusText;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setValuesFromXml(attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setValuesFromXml(attrs);
	}

	private void setValuesFromXml(AttributeSet attrs) {
		mMaxValue = attrs.getAttributeIntValue(androidns, "max", 100);
		mMinValue = attrs.getAttributeIntValue(chasinglemonsseekns, "min", 0);
		mUnits = attrs.getAttributeValue(chasinglemonsseekns, "units");
		try {
			String newInterval = attrs.getAttributeValue(chasinglemonsseekns, "interval");
			if(newInterval != null)
				mInterval = Integer.parseInt(newInterval);
		}
		catch(Exception e) {
			//Log.e(TAG, "Invalid interval value", e);
		}
		
	}
	
	@Override
	protected View onCreateView(ViewGroup parent){
		
		RelativeLayout layout =  null;
				
		try {
			LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			layout = (RelativeLayout)mInflater.inflate(R.layout.seek_bar_preference, parent, false);
			
			TextView title = (TextView)layout.findViewById(R.id.seekBarPrefTitle);
			title.setText(getTitle());
			
			TextView summary = (TextView)layout.findViewById(R.id.seekBarPrefSummary);
			summary.setText(getSummary());
			
			SeekBar seekBar = (SeekBar)layout.findViewById(R.id.seekBarPrefBar);
			seekBar.setMax(mMaxValue - mMinValue);
			seekBar.setProgress(mCurrentValue - mMinValue);
			seekBar.setOnSeekBarChangeListener(this);

			mStatusText = (TextView)layout.findViewById(R.id.seekBarPrefValue);
			mStatusText.setText(String.valueOf(mCurrentValue));
			mStatusText.setMinimumWidth(30);
			
			TextView units = (TextView)layout.findViewById(R.id.seekBarPrefUnits);
			units.setText(mUnits);
			
		}
		catch(Exception e) {
			//Log.e(TAG, "Error building seek bar preference", e);
		}

		return layout; 
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		int newValue = progress + mMinValue;
		
		if(newValue > mMaxValue)
			newValue = mMaxValue;
		else if(newValue < mMinValue)
			newValue = mMinValue;
		else if(mInterval != 1 && newValue % mInterval != 0)
			newValue = Math.round(((float)newValue)/mInterval)*mInterval;  
		
		// change rejected, revert to the previous value
		if(!callChangeListener(newValue)){
			seekBar.setProgress(mCurrentValue - mMinValue); 
			return; 
		}

		// change accepted, store it
		mCurrentValue = newValue;
		mStatusText.setText(String.valueOf(newValue));
		persistInt(newValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		notifyChanged();
	}


	@Override 
	protected Object onGetDefaultValue(TypedArray ta, int index){
		
		int defaultValue = ta.getInt(index, DEFAULT_VALUE);
		return defaultValue;
		
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		if(restoreValue) {
			mCurrentValue = getPersistedInt(mCurrentValue);
		}
		else {
			int temp = (Integer)defaultValue;
			persistInt(temp);
			mCurrentValue = temp;
		}
		
	}

}

