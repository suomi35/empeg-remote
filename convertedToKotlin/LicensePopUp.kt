package com.chasinglemons.empeg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button

class LicensePopUp : Activity(), View.OnClickListener {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.license_pop)
        title = resources.getString(R.string.app_name) + " licensing"
        val bExit = findViewById<View>(R.id.button_exit) as Button
        bExit.setOnClickListener(this)
        val bPurchase = findViewById<View>(R.id.button_purchase) as Button
        bPurchase.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_exit) {
            finish()
        } else if (v.id == R.id.button_purchase) {
            val marketIntent = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=$packageName"
                )
            )
            startActivity(marketIntent)
        }
    }
}
