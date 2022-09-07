package own.tracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private lateinit var edit: SharedPreferences.Editor

    companion object {
        private var seconds = 0
        private var running = false
        private var wasRunning = false

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


        if (savedInstanceState != null) {

            seconds = savedInstanceState.getInt("seconds")
            running = savedInstanceState.getBoolean("running")
            //wasRunning = savedInstanceState.getBoolean("wasRunning")
        }
        runTimer()


        val reset = findViewById<View>(R.id.reset_button) as Button
        reset.setOnClickListener {
            timerReset()
        }
        val phoneweight = resources.getStringArray(R.array.weight)
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        edit = preferences.edit()
        val sleepService = Intent(this, SleepService::class.java)


        val sleepServiceSwitch = findViewById<View>(R.id.sleepservice) as Switch
        sleepServiceSwitch.setChecked(preferences.getBoolean("sleepservice",false))
        sleepServiceSwitch?.setOnCheckedChangeListener({ _ , isChecked ->
            if (isChecked) {
                edit.putBoolean("sleepservice", true)
                edit.apply()
                startService(sleepService)
            } else {
                edit.putBoolean("sleepservice", false)
                edit.apply()
                stopService(sleepService)
                if (running) { running = false }
            }

        })


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

        val isDetectionEnabled = preferences.getBoolean("sleepservice", false)
        // Start Service - if enabled in prefernces.
        if (isDetectionEnabled) {
            val sleepService = Intent(this, SleepService::class.java)
            startService(sleepService)
        }


    }


    override fun onSaveInstanceState(
        savedInstanceState: Bundle
    ) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("seconds", seconds)
        savedInstanceState.putBoolean("running", running)
        //savedInstanceState.putBoolean("wasRunning", wasRunning)
    }

/*
    override fun onPause() {
        super.onPause()
        if (wasRunning) {
            running = true
        }
    }


    override fun onResume() {
        super.onResume()
        if (wasRunning) {
            running = true
        }
    }
*/

    private fun runTimer() {

        // Get the text view.
        val timeView = findViewById<View>(
            R.id.time_view
        ) as TextView

        // Creates a new Handler
        val handler = Handler()

        handler.post(object : Runnable {
            override fun run() {
                val hours: Int = seconds / 3600
                val minutes: Int = seconds % 3600 / 60
                val secs: Int = seconds % 60

                // Format the seconds into hours, minutes,
                // and seconds.
                val time: String = java.lang.String
                    .format(
                        Locale.getDefault(),
                        "%d:%02d:%02d", hours,
                        minutes, secs
                    )

                // Set the text view text.
                timeView.text = time
                if (running) {
                    seconds++
                }

                handler.postDelayed(this, 1000)
            }
        })
    }


}