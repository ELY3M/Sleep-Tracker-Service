package own.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.intentfilter.androidpermissions.PermissionManager
import com.intentfilter.androidpermissions.models.DeniedPermissions
import java.util.*
import java.util.Collections.singleton


class MainActivity : AppCompatActivity(), LocationListener {

    lateinit var locationManager: LocationManager
    var mylocation: Location? = null
    lateinit var sleepService: Intent
    lateinit var preferences: SharedPreferences
    lateinit var edit: SharedPreferences.Editor
    lateinit var sleepServiceSwitch: Button
    lateinit var gps: TextView

    companion object {
        var lat = 0.0
        var lon = 0.0
        var lat_set: String? = "0.0"
        var lon_set: String? = "0.0"
        var seconds = 0
        var running = false

        fun timerStart() {
            running = true
        }

        fun timerStop() {
            running = false
        }

        fun timerReset() {
            running = false
            seconds = 0
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //location permission
        val permissionManager = PermissionManager.getInstance(applicationContext)
        permissionManager.checkPermissions(singleton(Manifest.permission.ACCESS_FINE_LOCATION), object :
            PermissionManager.PermissionRequestListener {
            override fun onPermissionGranted() {
                Log.d("owntracker", "Great!!! Got Location Perms!. :)")
                getLocation()
                lon = mylocation!!.longitude
                lat = mylocation!!.latitude

            }

            override fun onPermissionDenied(deniedPermissions: DeniedPermissions) {
                Log.d("owntracker", "Permissions Denied")

            }
        })

        //background location permission
        val bgpermissionManager = PermissionManager.getInstance(applicationContext)
        bgpermissionManager.checkPermissions(singleton(Manifest.permission.ACCESS_BACKGROUND_LOCATION), object :
            PermissionManager.PermissionRequestListener {
            override fun onPermissionGranted() {
                Log.d("owntracker", "Great!!! Got Background Location Perms!. :)")
            }

            override fun onPermissionDenied(deniedPermissions: DeniedPermissions) {
                Log.d("owntracker", "Permissions Denied")

            }
        })

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        edit = preferences.edit()
        val isRunning = preferences.getBoolean("running", false)
        sleepService = Intent(this, SleepService::class.java)

        lon_set = preferences.getString("lon_set", "0.0")
        lat_set = preferences.getString("lat_set", "0.0")

        if (savedInstanceState != null) {
            seconds = savedInstanceState.getInt("seconds")
            running = savedInstanceState.getBoolean("running")
        }
        runTimer()


        val timeView = findViewById<View>(R.id.time_view) as TextView
        seconds = preferences.getInt("time", 0)
        val gethours: Int = seconds / 3600
        val getminutes: Int = seconds % 3600 / 60
        val getsecs: Int = seconds % 60
        val gettime: String = java.lang.String.format(Locale.getDefault(), "%d:%02d:%02d", gethours, getminutes, getsecs)
        timeView.text = gettime

        val reset = findViewById<View>(R.id.reset_button) as Button
        reset.setOnClickListener {
            timerReset()
            seconds = 0
            edit.putInt("time", 0)
        }
        val phoneweight = resources.getStringArray(R.array.weight)


        sleepServiceSwitch = findViewById<View>(R.id.sleepservice) as Button
        sleepServiceSwitch.setOnClickListener {
            if (running) {
                timerStop()
                running = false
                edit.putBoolean("running", false)
                edit.apply()
                stopService(sleepService)
                sleepServiceSwitch.text = "Start"
            } else {
                timerStart()
                running = true
                edit.putBoolean("running", true)
                edit.apply()
                startService(sleepService)
                sleepServiceSwitch.text = "Stop"
            }
        }

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

        //gps setting
        gps = findViewById<View>(R.id.gps) as TextView
        lon_set = preferences.getString("lon_set", "0.0")
        lat_set = preferences.getString("lat_set", "0.0")
        gps.text = "Current GPS: $lon $lat\nSet GPS: $lon_set $lat_set"
        val setgps = findViewById<View>(R.id.setgps) as Button
        setgps.setOnClickListener {
            gps.text = "Current GPS: $lon $lat\nSet GPS: ${lon.toString()} ${lat.toString()}"
            edit.putString("lon_set", lon.toString())
            edit.putString("lat_set", lat.toString())
            edit.apply()
        }


        if (isRunning) {
            timerStart()
            startService(sleepService)
            running = true
            sleepServiceSwitch.text =  "Stop"
        } else {
            timerStop()
            stopService(sleepService)
            running = false
            sleepServiceSwitch.text =  "Start"
        }


    }


    override fun onPause() {
        super.onPause()
        edit.putInt("time", seconds)
        Log.i("owntracker","onPause()!!!  seconds: $seconds")
    }

    override fun onDestroy() {
        super.onDestroy()
        edit.putInt("time", seconds)
        Log.i("owntracker","onDestory()!!!  seconds: $seconds")
    }

    override fun onSaveInstanceState(
        savedInstanceState: Bundle
    ) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("seconds", seconds)
        savedInstanceState.putBoolean("running", running)
    }


    private fun gpsCheck() {
        sleepService = Intent(this, SleepService::class.java)
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        edit = preferences.edit()
        lon_set = preferences.getString("lon_set", "0.0")
        lat_set = preferences.getString("lat_set", "0.0")
        sleepServiceSwitch = findViewById<View>(R.id.sleepservice) as Button
        Log.i("owntracker", "gpsCheck(): $lon $lat - Set GPS: $lon_set $lat_set")
        val results = FloatArray(1)
        Location.distanceBetween(lat_set!!.toDouble(), lon_set!!.toDouble(), lat, lon, results)
        val dist = results[0]
        Log.i("owntracker", "gps dist: $dist")
        if (dist > 1) {
            Log.i("owntracker", "Phone GPS Moved Too far!!!!! $dist")
            if (running) {
                timerStop()
                running = false
                edit.putBoolean("running", false)
                edit.apply()
                stopService(sleepService)
                sleepServiceSwitch.text = "Start"
            }
        } else {
            Log.i("owntracker", "Phone sleeping/staying at set GPS: $dist")
            if (!running) {
                timerStart()
                running = true
                edit.putBoolean("running", true)
                edit.apply()
                startService(sleepService)
                sleepServiceSwitch.text = "Stop"
            }
        }


    }


    private fun runTimer() {
        val timeView = findViewById<View>(R.id.time_view) as TextView
        edit = preferences.edit()
        val handler = Handler()
        seconds = preferences.getInt("time", 0)
        handler.post(object : Runnable {
            override fun run() {
                val hours: Int = seconds / 3600
                val minutes: Int = seconds % 3600 / 60
                val secs: Int = seconds % 60
                var time: String = java.lang.String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
                timeView.text = time
                edit.putInt("time", seconds)
                if (running) {
                    getLocation()
                    sleepServiceSwitch.text = "Stop"
                    seconds++
                    edit.putInt("time", seconds)
                } else {
                    getLocation()
                    sleepServiceSwitch.text = "Start"
                    edit.putInt("time", seconds)
                }

                handler.postDelayed(this, 1000)
            }
        })
    }


    /////GPS Stuff//////
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 5000, 5f, this)
        mylocation = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)

        if (mylocation != null) {
            lat = mylocation!!.latitude
            lon = mylocation!!.longitude
            Log.d("owntracker", "Lat: " + lat)
            Log.d("owntracker", "Lon: " + lon)
            gpsCheck()
        } else {
            Log.d("owntracker", "mylocation is null :(")
        }


    }

    override fun onLocationChanged(location: Location) {
        mylocation = location
        lat = mylocation!!.latitude
        lon = mylocation!!.longitude
        lon_set = preferences.getString("lon_set", "0.0")
        lat_set = preferences.getString("lat_set", "0.0")
        gps.text = "Current GPS: $lon $lat\nSet GPS: $lon_set $lat_set"
        Log.d("owntracker", "Lat: "+ lat)
        Log.d("owntracker", "Lon: "+ lon)
    }
/////end of gps stuff/////

}