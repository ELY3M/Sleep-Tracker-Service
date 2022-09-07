/*
 * Copyright 2022 ELY M. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package own.tracker

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import kotlin.math.sqrt

@Suppress("DEPRECATION")
class SleepService : Service(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mAccel = 0f
    private var mAccelCurrent = 0f
    private var mAccelLast = 0f

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
        val sleeptrack = preferences.getBoolean("sleeptrack", false)
        val sensitive = preferences.getString("phoneweight", "6.3")
        //Log.i("owntracker", "sensitive (phone weight): $sensitive")
        val sensitivef = sensitive!!.toFloat()
        Log.i("owntracker", "mAccel: $mAccel")
        if (mAccel > sensitivef) {
            if (sleeptrack) {
                Log.i("owntracker", "SleepService: phone movement: " + mAccel.toString())
                val editor = preferences.edit()
                editor.putBoolean("sleeptrack", false)
                editor.apply()
                Log.i("owntracker", "SleepService: sleeptrack after phone moved: " + sleeptrack)
                Log.i("owntracker", "sleep tracking should stop")
                MainActivity.timerStop()
            }
        } else {
            MainActivity.timerStart()
            val editor = preferences.edit()
            editor.putBoolean("sleeptrack", true)
            editor.apply()
        }
    }
}
