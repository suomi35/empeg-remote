package com.chasinglemons.empeg;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LicensePopUp extends Activity implements OnClickListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.license_pop);
		setTitle(getResources().getString(R.string.app_name)+" licensing");
		Button bExit = (Button) findViewById(R.id.button_exit);
		bExit.setOnClickListener(this);
		Button bPurchase = (Button) findViewById(R.id.button_purchase);
		bPurchase.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button_exit) {
			finish();
		} else if (v.getId() == R.id.button_purchase) {
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
					"https://play.google.com/store/apps/details?id=" + getPackageName()));
			startActivity(marketIntent);
		}
	}
}
