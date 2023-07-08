/*
 * Copyright 2022 ELY M.
 * Fuck copyright!!!!
 */

package own.tracker

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.sqrt


@Suppress("DEPRECATION")
class SleepService : Service(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mAccel = 0f
    private var mAccelCurrent = 0f
    private var mAccelLast = 0f

    private var ServiceGetTimer: String = ""

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorManager!!.registerListener(
            this, mAccelerometer, SensorManager.SENSOR_DELAY_UI, Handler()
        )
        return START_STICKY
    }

    fun countTime(startTimeStr: String): String {
        //get now date in GMT zone
        val cal: Calendar = Calendar.getInstance()
        val format = SimpleDateFormat("h:mm:ss a")

        val nowDateTime = format.format(cal.getTime())
        val startTime = format.parse(startTimeStr)
        val now = format.parse(nowDateTime)

        Log.d("owntracker", "startTimeStr: "+startTimeStr)
        Log.d("owntracker", "nowDateTime: "+nowDateTime)
        Log.d("owntracker", "startTime: "+startTime)
        Log.d("owntracker", "now: "+now)

        //in milliseconds
        val diff = now.getTime() - startTime.getTime()
        val diffSeconds = diff / 1000 % 60
        val diffMinutes = diff / (60 * 1000) % 60
        val diffHours = diff / (60 * 60 * 1000) % 24
        val diffDays = diff / (24 * 60 * 60 * 1000)
        ///val totalTime = "$diffDays days, $diffHours hrs, $diffMinutes mins, $diffSeconds secs"
        var totalTime = "time: $diffHours:$diffMinutes:$diffSeconds"
        if (diffDays <= 0) {
            totalTime = "$diffHours:$diffMinutes:$diffSeconds"
        } else {
            totalTime = "$diffDays days $diffHours:$diffMinutes:$diffSeconds"
        }
        Log.d("owntracker", "totalTime: "+totalTime)
        return totalTime
    }

    fun getTimeNow(): String {

        val timestamp = SimpleDateFormat("h:mm:ss a")
        val c = Calendar.getInstance()
        val mytimestamp = timestamp.format(c.time)
        Log.i("owntracker", "mytimestamp: "+mytimestamp)
        return mytimestamp

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

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        val x: Float = event.values.get(0)
        val y: Float = event.values.get(1)
        val z: Float = event.values.get(2)
        mAccelLast = mAccelCurrent
        mAccelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = mAccelCurrent - mAccelLast
        mAccel = mAccel * 0.9f + delta // perform low-cut filter
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val running = preferences.getBoolean("running", false)
        val sensitive = preferences.getString("phoneweight", "6.3")
        //Log.i("owntracker", "sensitive (phone weight): $sensitive")
        val sensitivef = sensitive!!.toFloat()
        //Log.i("owntracker", "sensitivef (phone weight): $sensitivef")
        //Log.i("owntracker", "mAccel: $mAccel")
        if (mAccel > sensitivef) {
            if (running) {
                Log.i("owntracker", "SleepService: phone movement: " + mAccel.toString())
                val editor = preferences.edit()
                editor.putBoolean("running", false)
                editor.apply()
                Log.i("owntracker", "SleepService: sleeptrack after phone moved: " + running)
                Log.i("owntracker", "SleepService: sleep tracking should stop")
                MainActivity.running = false
                MainActivity.endtime = getTimeNow()

                val pm = getSystemService(POWER_SERVICE) as PowerManager
                if (pm.isInteractive()) {
                    Log.i("owntracker", "SleepService: Phone is on and awake")
                    ServiceGetTimer = countTime(MainActivity.starttime)
                    val timestring = MainActivity.starttime +" to "+ MainActivity.endtime +" = "+ServiceGetTimer+" ("+MainActivity.getTimer+")\n"
                    Log.i("owntracker", "SleepService: " + timestring)
                    writeLog(timestring)
                } else {
                    Log.i("owntracker", "SleepService: Phone is asleep")
                    ServiceGetTimer = countTime(MainActivity.starttime)
                    val timestring = MainActivity.starttime +" to "+ MainActivity.endtime +" = "+ServiceGetTimer+" ("+MainActivity.getTimer+")\n"
                    Log.i("owntracker", "SleepService: " + timestring)
                    writeLog(timestring)
                }

                val intent = Intent("timer")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)




            }
        } /*else {
            val editor = preferences.edit()
            editor.putBoolean("running", true)
            editor.apply()
            MainActivity.timerStart()
            MainActivity.running = true
        }*/
    }

}
