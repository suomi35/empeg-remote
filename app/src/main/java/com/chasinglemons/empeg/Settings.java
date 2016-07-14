package com.chasinglemons.empeg;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.WindowManager;

public class Settings extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    addPreferencesFromResource(R.xml.settings);
  }
}