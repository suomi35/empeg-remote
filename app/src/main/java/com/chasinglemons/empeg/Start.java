package com.chasinglemons.empeg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;

public class Start extends Activity {

	double tabletMinimum = 6;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isTablet()) {
			Intent tabVersion = new Intent(this, TabletMain.class);
			tabVersion.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(tabVersion);
			finish();
		} else {
			Intent phoneVersion = new Intent(this, PhoneMain.class);
			phoneVersion.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(phoneVersion);
			finish();
		}
	}

	public boolean isTablet() { 
		try { 
			// Compute screen size 
			DisplayMetrics dm = this.getResources().getDisplayMetrics(); 
			float screenWidth  = dm.widthPixels / dm.xdpi; 
			float screenHeight = dm.heightPixels / dm.ydpi; 
			double size = Math.sqrt(Math.pow(screenWidth, 2) + 
					Math.pow(screenHeight, 2));

			// Tablet devices should have a screen size greater than 6 inches 
			return size >= tabletMinimum; 
		} catch(Throwable t) { 
//			Log.e("START", "Failed to compute screen size", t);
			return false; 
		}
	}
}
