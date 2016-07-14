package com.chasinglemons.empeg;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chasinglemons.empeg.Discoverer.DiscoveryReceiver;

public class AddEmpeg extends Activity implements DiscoveryReceiver,OnCancelListener {

	SharedPreferences config;
	ArrayList<Empeg> returnedEmpegs;
	ProgressBar progresso;
	public final static int SUCCESS_RETURN_CODE = 1;
	public final static int CANCEL_RETURN_CODE = 0;
	int activeLens;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_empeg);
		
		config = PreferenceManager.getDefaultSharedPreferences(AddEmpeg.this);
		
		activeLens = Integer.parseInt(config.getString("activeLens","0"));

		TextView manualLabel = (TextView) findViewById(R.id.textView1);
		final EditText manualEntry = (EditText) findViewById(R.id.manual_ip_entry);

		if (!config.getString("activeEmpegIP", "none").equals("none")) {
			manualLabel.setText("Currently controlling empeg at:");
			manualEntry.setText(config.getString("activeEmpegIP", "none"));
		}

		progresso = (ProgressBar) findViewById(getProgressBar());
		progresso.setVisibility(View.VISIBLE);
		new Discoverer((WifiManager) getSystemService(Context.WIFI_SERVICE), this, this).start();


		Button manualAdd = (Button) findViewById(R.id.manual_ip_button);
		manualAdd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isEmpty(manualEntry)) {
					SharedPreferences.Editor editor = config.edit();
					editor.putString("activeEmpegName", "");
					editor.putString("activeEmpegIP", manualEntry.getText().toString());
					editor.commit();
					finish();
				}
			}
		});

		getActionBar().setDisplayHomeAsUpEnabled(true);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter("finish-add"));
	}

	private boolean isEmpty(EditText etText) {
		if (etText.getText().toString().trim().length() > 0) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void addAnnouncedServers(ArrayList<Empeg> servers) {
		returnedEmpegs = servers;

		runOnUiThread(new Runnable() {
			@Override
			public void run(){
				ListView empList = (ListView)findViewById(R.id.empeg_list);
				ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
				HashMap<String,String> item;

				for (int i = 0; i < returnedEmpegs.size(); i++) {
					item = new HashMap<String,String>();
					item.put("line1", returnedEmpegs.get(i).getempegName());
					item.put("line2", "("+returnedEmpegs.get(i).getempegIP()+")");
					list.add(item);
				}

				SimpleAdapter sa = new SimpleAdapter(AddEmpeg.this, list,
						R.layout.my_two_lines,
						new String[] { "line1","line2" },
						new int[] {R.id.line_a, R.id.line_b});
				empList.setAdapter(sa);

				empList.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						//Log.i("ADD_EMPEG","you likie "+returnedEmpegs.get(position).getempegName()+"("+returnedEmpegs.get(position).getempegIP()+")");
						// save the empeg details
						SharedPreferences.Editor editor = config.edit();
						editor.putString("activeEmpegName", returnedEmpegs.get(position).getempegName());
						editor.putString("activeEmpegIP", returnedEmpegs.get(position).getempegIP());
						editor.commit();
						finish();
					}
				});
				progresso.setVisibility(View.GONE);
			}
		});
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		Intent intent = new Intent();
		setResult(CANCEL_RETURN_CODE, intent);
		finish();		
	}

	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "finish-add" is broadcast.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_empeg_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId()) {
		case R.id.action_refresh:
			progresso.setVisibility(View.VISIBLE);
			new Discoverer((WifiManager) getSystemService(Context.WIFI_SERVICE), this, this).start();
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, Settings.class));
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private int getProgressBar() {
		activeLens = Integer.parseInt(config.getString("activeLens","0"));
		switch (activeLens) {
		case 0:
			return R.id.blue_progressbar;
		case 1:
			return R.id.red_progressbar;
		case 2:
			return R.id.yellow_progressbar;
		case 3:
			return R.id.green_progressbar;
		case 4:
			return R.id.white_progressbar;
		default:
			return R.id.blue_progressbar;
		}
	}
}
