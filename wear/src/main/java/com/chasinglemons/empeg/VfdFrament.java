package com.chasinglemons.empeg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class VfdFrament extends Fragment implements View.OnClickListener {

    private final static int VFD_UPDATE_MSEC = 5000;
    private MainActivity activity;
    private TextView vfdStatusText;
    private TextView vfdArtistText;
    private TextView vfdTrackText;
    private Handler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_vfd_frament, container, false);

        activity = (MainActivity) getActivity();

        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),"fonts/pixelmix.ttf");

        vfdStatusText = (TextView) rootView.findViewById(R.id.vfd_status);
        vfdStatusText.setTypeface(tf);

        vfdArtistText = (TextView) rootView.findViewById(R.id.vfd_artist_name);
        vfdArtistText.setTypeface(tf);

        vfdTrackText = (TextView) rootView.findViewById(R.id.vfd_track_name);
        vfdTrackText.setTypeface(tf);

        ImageView vfd = (ImageView) rootView.findViewById(R.id.empeg_vfd);
        vfd.setOnClickListener(this);

        handler = new Handler();

        return rootView;
    }

    @Override
    public void onResume() {
        Log.d("VFD", "onResume()");
        super.onResume();
        LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver,
                new IntentFilter("screen-update"));
        handler.postDelayed(getVFDData, VFD_UPDATE_MSEC);
    }

    @Override
    public void onPause() {
        Log.d("VFD", "onPause()");
        super.onPause();
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mMessageReceiver);
        handler.removeCallbacks(getVFDData);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.empeg_vfd:
                requestScreen();
                break;
        }
    }

    private void requestScreen() {
            Log.d("VFD", "requesting VFD update");
            activity.getVFDUpdate(MainActivity.WEAR_MESSAGE_PATH, "get_vfd");
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcast.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if (intent.getStringExtra("updatedScreen") != null) {
                String[] vfdParts = intent.getStringExtra("updatedScreen").split(":#:");
                for (String stg : vfdParts) {
                    Log.d("VFD", "stg: " + stg);
                }

                if (vfdParts[2].equals("comm error")) {
                    vfdStatusText.setText("communication\berror");
                } else {
                    if (vfdParts[2].equals("0")) {
                        vfdStatusText.setText("Paused");
                    } else {
                        vfdStatusText.setText("Now Playing");
                    }
                    vfdArtistText.setText(vfdParts[1]);
                    vfdTrackText.setText(vfdParts[3]);
                    vfdTrackText.setSelected(true);
                }
            }
        }
    };

    private final Runnable getVFDData = new Runnable() {
        public void run() {
            try {
                requestScreen();
                handler.postDelayed(this, VFD_UPDATE_MSEC);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
