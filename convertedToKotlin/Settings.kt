package com.chasinglemons.empeg

import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.WindowManager

class Settings : PreferenceActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )
        addPreferencesFromResource(R.xml.settings)
    }
}