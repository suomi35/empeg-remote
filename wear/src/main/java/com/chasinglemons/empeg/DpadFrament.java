package com.chasinglemons.empeg;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


public class DpadFrament extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_dpad_frament, container, false);

        activity = (MainActivity) getActivity();

        ImageButton btnUp = (ImageButton) rootView.findViewById(R.id.button_up);
        btnUp.setOnClickListener(this);
        btnUp.setOnLongClickListener(this);
        ImageButton btnLeft = (ImageButton) rootView.findViewById(R.id.button_left);
        btnLeft.setOnClickListener(this);
        btnLeft.setOnLongClickListener(this);
        ImageButton btnRight = (ImageButton) rootView.findViewById(R.id.button_right);
        btnRight.setOnClickListener(this);
        btnRight.setOnLongClickListener(this);
        ImageButton btnDown = (ImageButton) rootView.findViewById(R.id.button_down);
        btnDown.setOnClickListener(this);
        btnDown.setOnLongClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_up:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Top");
                break;
            case R.id.button_left:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Left");
                break;
            case R.id.button_right:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Right");
                break;
            case R.id.button_down:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Bottom");
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.button_up:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Top.L");
                break;
            case R.id.button_left:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Left.L");
                break;
            case R.id.button_right:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Right.L");
                break;
            case R.id.button_down:
                activity.sendMessage(MainActivity.WEAR_MESSAGE_PATH, "/proc/empeg_notify?button=Bottom.L");
                break;
        }
        return true;
    }
}
