package com.chasinglemons.empeg;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WidgetConfigure extends Activity {
	private Context self = this;
	private int appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get the appWidgetId of the appWidget being configured
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);

		// set the result for cancel first
		Intent cancelResultValue = new Intent();
		cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				appWidgetId);
		setResult(RESULT_CANCELED, cancelResultValue);
		setContentView(R.layout.widget_configure);

		// the OK button
		Button ok = (Button) findViewById(R.id.button_ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {

				// fire an update to display initial state of the widget
				PendingIntent updatepending = Widget
						.makeControlPendingIntent(self,
								WidgetService.UPDATE, appWidgetId);
				try {
					updatepending.send();
				} catch (CanceledException e) {
					e.printStackTrace();
				}
				// change the result to OK
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				setResult(RESULT_OK, resultValue);
				finish();
			}
		});

		// cancel button
		Button cancel = (Button) findViewById(R.id.button_cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
	}

}
