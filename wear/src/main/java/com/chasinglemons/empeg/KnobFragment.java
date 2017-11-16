package com.chasinglemons.empeg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


public class KnobFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {
    MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_knob_frament, container, false);

        activity = (MainActivity) getActivity();

        ImageButton knobLeft = (ImageButton) rootView.findViewById(R.id.knob_left);
        knobLeft.setOnClickListener(this);
        ImageButton knobRight = (ImageButton) rootView.findViewById(R.id.knob_right);
        knobRight.setOnClickListener(this);
//        ImageButton btnCenter = (ImageButton) rootView.findViewById(R.id.button_right);
//        btnCenter.setOnClickListener(this);
//        btnCenter.setOnLongClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.knob_left:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=VolDown");
                break;
            case R.id.knob_right:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=VolUp");
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.button_up:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Top.L");
                break;
        }
        return true;
    }
}