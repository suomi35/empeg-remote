package com.chasinglemons.empeg

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chasinglemons.empeg.Discoverer.DiscoveryReceiver

class AddEmpeg : Activity(), DiscoveryReceiver, DialogInterface.OnCancelListener {
    var config: SharedPreferences? = null
    var returnedEmpegs: ArrayList<Empeg>? = null
    var progresso: ProgressBar? = null
    var activeLens: Int = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_empeg)

        config = PreferenceManager.getDefaultSharedPreferences(this@AddEmpeg)

        activeLens = config.getString("activeLens", "0")!!.toInt()

        val manualLabel = findViewById<View>(R.id.textView1) as TextView
        val manualEntry = findViewById<View>(R.id.manual_ip_entry) as EditText

        if (config.getString("activeEmpegIP", "none") != "none") {
            manualLabel.text = "Currently controlling empeg at:"
            manualEntry.setText(config.getString("activeEmpegIP", "none"))
        }

        progresso = findViewById<View>(progressBar) as ProgressBar
        progresso!!.visibility = View.VISIBLE
        Discoverer(
            applicationContext.getSystemService(WIFI_SERVICE) as WifiManager,
            this,
            this
        ).start()


        val manualAdd = findViewById<View>(R.id.manual_ip_button) as Button
        manualAdd.setOnClickListener {
            if (!isEmpty(manualEntry)) {
                val editor = config.edit()
                editor.putString("activeEmpegName", "")
                editor.putString("activeEmpegIP", manualEntry.text.toString())
                editor.commit()
                finish()
            }
        }

        actionBar!!.setDisplayHomeAsUpEnabled(true)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("finish-add"))
    }

    private fun isEmpty(etText: EditText): Boolean {
        return if (etText.text.toString().trim { it <= ' ' }.length > 0) {
            false
        } else {
            true
        }
    }

    override fun addAnnouncedServers(servers: ArrayList<Empeg>?) {
        returnedEmpegs = servers

        runOnUiThread {
            val empList =
                findViewById<View>(R.id.empeg_list) as ListView
            val list = ArrayList<HashMap<String, String?>>()
            var item: HashMap<String, String?>

            for (i in returnedEmpegs!!.indices) {
                item = HashMap()
                item["line1"] = returnedEmpegs!![i].getempegName()
                item["line2"] = "(" + returnedEmpegs!![i].getempegIP() + ")"
                list.add(item)
            }

            val sa = SimpleAdapter(
                this@AddEmpeg, list,
                R.layout.my_two_lines,
                arrayOf("line1", "line2"),
                intArrayOf(R.id.line_a, R.id.line_b)
            )
            empList.adapter = sa

            empList.onItemClickListener =
                OnItemClickListener { parent, view, position, id -> //Log.i("ADD_EMPEG","you likie "+returnedEmpegs.get(position).getempegName()+"("+returnedEmpegs.get(position).getempegIP()+")");
                    // save the empeg details
                    val editor = config!!.edit()
                    editor.putString(
                        "activeEmpegName",
                        returnedEmpegs!![position].getempegName()
                    )
                    editor.putString(
                        "activeEmpegIP",
                        returnedEmpegs!![position].getempegIP()
                    )
                    editor.commit()
                    finish()
                }
            progresso!!.visibility = View.GONE
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        val intent = Intent()
        setResult(CANCEL_RETURN_CODE, intent)
        finish()
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "finish-add" is broadcast.
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_empeg_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_refresh) {
            progresso!!.visibility = View.VISIBLE
            Discoverer(
                applicationContext.getSystemService(WIFI_SERVICE) as WifiManager,
                this,
                this
            ).start()
            return true
        } else if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, Settings::class.java))
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private val progressBar: Int
        get() {
            activeLens = config!!.getString("activeLens", "0")!!.toInt()
            return when (activeLens) {
                0 -> R.id.blue_progressbar
                1 -> R.id.red_progressbar
                2 -> R.id.yellow_progressbar
                3 -> R.id.green_progressbar
                4 -> R.id.white_progressbar
                else -> R.id.blue_progressbar
            }
        }

    companion object {
        const val SUCCESS_RETURN_CODE: Int = 1
        const val CANCEL_RETURN_CODE: Int = 0
    }
}
