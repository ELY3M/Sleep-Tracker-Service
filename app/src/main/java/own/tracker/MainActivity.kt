/*
 * Copyright 2022 ELY M.
 * Fuck copyright!!!!
 */

package own.tracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Chronometer
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.markodevcic.peko.PermissionRequester
import com.markodevcic.peko.PermissionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity()/*, SensorEventListener*/ {

    //private var sensorMan: SensorManager? = null
    //private var accelerometer: Sensor? = null
    //private var mAccel = 0.0
    //private var mAccelCurrent = 0.0
    //private var mAccelLast = 0.0
    lateinit var sleepService: Intent
    lateinit var preferences: SharedPreferences
    lateinit var edit: SharedPreferences.Editor
    lateinit var timer: Chronometer
    lateinit var startbutton: Button
    lateinit var sleeplogtext: TextView
    var starttime: String = ""
    var endtime: String = ""
    val handler = Handler(Looper.getMainLooper())

    companion object {
        var running = false
        var stoppedtime: Long = 0

    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.i("owntracker", "got broadcast to stop timer")
            preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            edit = preferences.edit()
            startbutton = findViewById<View>(R.id.sleepservice) as Button
            timer.stop()
            stoppedtime = timer.getBase() - SystemClock.elapsedRealtime()
            edit.putLong("stopped", stoppedtime)
            edit.apply()
            running = false
            edit.putBoolean("running", false)
            edit.apply()
            startbutton.text = "Start"
            endtime = getTime()
            val timer = findViewById<View>(R.id.timer) as Chronometer
            val getTimer = timer.text
            ///val timestring = "$starttime $getTimer $endtime\n"
            val timestring = "$starttime to $endtime = $getTimer\n"
            Log.i("owntracker", timestring)
            writeLog(timestring)
        }
    }

    override fun onStart() {
        super.onStart()
        sleepService = Intent(this, SleepService::class.java)
        startService(sleepService)
        val intentFilter = IntentFilter()
        intentFilter.addAction("timer");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun askPerms() {
        PermissionRequester.initialize(applicationContext)
        val requester = PermissionRequester.instance()
        Log.i("owntracker","running askPerms()")

        if(SDK_INT >= 30) {
            Log.i("owntracker","newer phone: have write perms!")
            askExternalStorageManager()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                requester.request(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).collect { p ->
                    when (p) {
                        is PermissionResult.Granted -> {
                            Log.i("owntracker","older phone: have write perms!")
                        }
                        else -> {}
                    }
                }
            }
        }
    }


    fun askExternalStorageManager() {
        if(SDK_INT >= 30) {
            if (Environment.isExternalStorageManager()) {
                Log.i("owntracker", "ExternalStorageManager Perms are already granted :)")
            } else {
                Toast.makeText(applicationContext, "This app need access to your phone memory or SD Card to make files and write files (/wX/ on your phone memory or sd card)\nThe all file access settings will open. Make sure to toggle it on to enable all files access for this app to function fully.\n You need to restart the app after you enabled the all files access for this app in the settings.\n", Toast.LENGTH_LONG).show()
                val permissionIntent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(permissionIntent)
                Thread.sleep(13000) //sleep for 13 secs and force restart
                exitProcess(0)
            }
        }
    }


    fun getTime(): String {

        val timestamp = SimpleDateFormat("h:mm:ss a")
        val c = Calendar.getInstance()
        val mytimestamp = timestamp.format(c.time)
        Log.i("owntracker", "mytimestamp: "+mytimestamp)
        return mytimestamp

    }

    fun readLog(): String {
        val sdcard = Environment.getExternalStorageDirectory()
        val dir = File(sdcard.absolutePath + "/")
        val file = File(dir, "sleeplog.txt")
        val inputStream: InputStream = file.inputStream()
        val gettext = inputStream.bufferedReader().use { it.readText() }
        return gettext
    }

    fun writeLog(log: String) {
        val sdcard = Environment.getExternalStorageDirectory()
        val dir = File(sdcard.absolutePath + "/")
        val file = File(dir, "sleeplog.txt")

        try {
            val file_writer = OutputStreamWriter(FileOutputStream(file, true))
            val buffered_writer = BufferedWriter(file_writer)
            buffered_writer.write(log)
            buffered_writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun deleteLog() {

        val sdcard = Environment.getExternalStorageDirectory()
        val dir = File(sdcard.absolutePath + "/")
        val file = File(dir, "sleeplog.txt")
        try {
            if (file.exists()) {
                file.delete()
                Log.i("owntracker", "Deletion succeeded.")
            } else {
                Log.i("owntracker", "Deletion failed - file not found")
            }
        } catch (e: IOException) {
            Log.i("owntracker", "Deletion failed.")
            e.printStackTrace()
        }

}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sleepService = Intent(this, SleepService::class.java)
        startService(sleepService)

        //FUCK YOU GOOGLE!!!!!!
        askPerms()

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        edit = preferences.edit()

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean("running")
        }

        runTimer()

        timer = findViewById<View>(R.id.timer) as Chronometer
        val font = Typeface.createFromAsset(assets, "Digital.ttf")
        timer.typeface = font


        startbutton = findViewById<View>(R.id.sleepservice) as Button
        if (running) { startbutton.text = "Stop" } else { startbutton.text = "Start" }
        startbutton.setOnClickListener {
            if (running) {
                timer.stop()
                stoppedtime = timer.getBase() - SystemClock.elapsedRealtime()
                edit.putLong("stopped", stoppedtime)
                edit.apply()
                running = false
                edit.putBoolean("running", false)
                edit.apply()
                startbutton.text = "Start"
                endtime = getTime()
                val timer = findViewById<View>(R.id.timer) as Chronometer
                val getTimer = timer.text
                ///val timestring = "$starttime $getTimer $endtime\n"
                val timestring = "$starttime to $endtime = $getTimer\n"
                Log.i("owntracker", timestring)
                writeLog(timestring)
            } else {
                timer.setBase(SystemClock.elapsedRealtime() + stoppedtime)
                edit.putLong("stopped", stoppedtime)
                edit.apply()
                timer.start()
                running = true
                edit.putBoolean("running", true)
                edit.apply()
                startbutton.text = "Stop"
                starttime = getTime()

            }
        }


        val reset = findViewById<View>(R.id.reset_button) as Button
        reset.setOnClickListener {
            stoppedtime = 0
            timer.setBase(SystemClock.elapsedRealtime())
            edit.putLong("stopped", stoppedtime)
            edit.apply()
            timer.stop()
            running = false
            edit.putBoolean("running", false)
            edit.apply()
            startbutton.text = "Start"
            endtime = getTime()
            val timer = findViewById<View>(R.id.timer) as Chronometer
            val getTimer = timer.text
            ///val timestring = "$starttime $getTimer $endtime\n"
            val timestring = "$starttime to $endtime = $getTimer\n"
            Log.i("owntracker", timestring)
            writeLog(timestring)
        }

        val phoneweight = resources.getStringArray(R.array.weight)
        val phoneWeight = findViewById<View>(R.id.phoneweight) as Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, phoneweight)
        phoneWeight.adapter = adapter

        val selected = preferences.getString("phoneweight", "6.5")
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        phoneWeight.setAdapter(adapter)
        if (selected != null) {
            val spinnerPosition = adapter.getPosition(selected)
            phoneWeight.setSelection(spinnerPosition)
        }


        phoneWeight.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val text: String = parent?.getItemAtPosition(position).toString()
                Log.i("owntracker", "choosen item: $text")
                edit.putString("phoneweight", text)
                edit.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }

        sleeplogtext = findViewById<View>(R.id.sleeplog) as TextView
        sleeplogtext.text = readLog()

        val refreshlog = findViewById<View>(R.id.refreshlog) as Button
        refreshlog.setOnClickListener {
            sleeplogtext.text = readLog()
        }

        val deletelog = findViewById<View>(R.id.deletelog) as Button
        deletelog.setOnClickListener {
            deleteLog()
            sleeplogtext.text = ""
        }


    }

    override fun onPause() {
        super.onPause()
        Log.i("owntracker", "onPause() running: $running")
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        edit = preferences.edit()
        timer = findViewById<View>(R.id.timer) as Chronometer
        startbutton = findViewById<View>(R.id.sleepservice) as Button
        if (running) { startbutton.text = "Stop" } else { startbutton.text = "Start" }
        if (running) {
            timer.start()
            running = true
            edit.putBoolean("running", true)
            edit.apply()
            startbutton.text = "Stop"
            starttime = getTime()
        }
    }


    override fun onResume() {
        super.onResume()
        Log.i("owntracker", "onResume() running: $running")
        val timer = findViewById<View>(R.id.timer) as Chronometer
        if (running) {
            stoppedtime = timer.getBase() - SystemClock.elapsedRealtime()
        } else {
            timer.setBase(SystemClock.elapsedRealtime() + stoppedtime)
        }
    }

/*
    override fun onDestroy() {
        super.onDestroy()
        timer = findViewById<View>(R.id.timer) as Chronometer
        sensorMan?.unregisterListener(this)
        timer.stop()
        stoppedtime = timer.getBase() - SystemClock.elapsedRealtime()
        edit.putLong("stopped", stoppedtime)
        edit.apply()
        running = false
        edit.putBoolean("running", false)
        edit.apply()
    }
*/


    override fun onSaveInstanceState(
        savedInstanceState: Bundle
    ) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("running", running)
    }

    val runnable: Runnable = object : Runnable {
        override fun run() {
            val timer = findViewById<View>(R.id.timer) as Chronometer
            val getTimer = timer.text
            Log.i("owntrackerwatch", "timer: $getTimer running: $running")
            preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            edit = preferences.edit()
            edit.putString("time", getTimer.toString())
            edit.apply()
            handler.postDelayed(this, 1000)
        }
    }

    private fun runTimer() {
        handler.removeCallbacks(runnable)
        runnable.run()
    }

}