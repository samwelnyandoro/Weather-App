package com.weatherapp.weatherapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.weatherapp.weatherapp.R
import com.weatherapp.weatherapp.adapter.MainAdapter
import com.weatherapp.weatherapp.api.Apiservice
import com.weatherapp.weatherapp.databinding.ActivityMainBinding
import com.weatherapp.weatherapp.databinding.ToolbarBinding
import com.weatherapp.weatherapp.fragments.FragmentNextDays
import com.weatherapp.weatherapp.model.ModelMain
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {
    private var lat: Double? = null
    private var lng: Double? = null
    private var today: String? = null
    private var mProgressBar: ProgressDialog? = null
    private var mainAdapter: MainAdapter? = null
    private val modelMain: MutableList<ModelMain> = ArrayList()
    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarBinding: ToolbarBinding
    var permissionArrays = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbarBinding = ToolbarBinding.bind(findViewById(R.id.toolbarLayout))
        val myVersion = Build.VERSION.SDK_INT
        if (myVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkIfAlreadyhavePermission() && checkIfAlreadyhavePermission2()) {
            } else {
                requestPermissions(permissionArrays, 101)
            }
        }

        val dateNow = Calendar.getInstance().time
        today = DateFormat.format("EEE", dateNow) as String

        mProgressBar = ProgressDialog(this).apply {
            setTitle("Please wait")
            setCancelable(false)
            setMessage("Currently displaying data...")
        }

        val fragmentNextDays = FragmentNextDays.newInstance("FragmentNextDays")
        mainAdapter = MainAdapter(modelMain)

        binding.rvListWeather.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            setHasFixedSize(true)
            adapter = mainAdapter
        }

        binding.fabNextDays.setOnClickListener {
            fragmentNextDays.show(supportFragmentManager, fragmentNextDays.tag)
        }
        getToday()
        getLatlong()
    }

    private fun getToday() {
        val date = Calendar.getInstance().time
        val tdate = DateFormat.format("d MMM yyyy", date) as String
        val formatDate = "$today, $tdate"
        binding.tvDate.text = formatDate
    }

    private fun getLatlong() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                115
            )
            return
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)
        val location = locationManager.getLastKnownLocation(provider.toString())
        if (location != null) {
            onLocationChanged(location)
        } else {
            locationManager.requestLocationUpdates(provider.toString(), 20000, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        lng = location.longitude
        lat = location.latitude
        getCurrentWeather()
        getListWeather()
    }

    private fun getCurrentWeather() {
        AndroidNetworking.get(
            Apiservice.BASEURL + Apiservice.CurrentWeather + "lat=" + lat + "&lon=" + lng + Apiservice.UnitsAppid
        )
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val jsonArrayOne = response.getJSONArray("weather")
                        val jsonObjectOne = jsonArrayOne.getJSONObject(0)
                        val jsonObjectTwo = response.getJSONObject("main")
                        val jsonObjectThree = response.getJSONObject("wind")
                        val strWeather = jsonObjectOne.getString("main")
                        val strDescWeather = jsonObjectOne.getString("description")
                        val strWindvelocity = jsonObjectThree.getString("speed")
                        val strHumidity = jsonObjectTwo.getString("humidity")
                        val strName = response.getString("name")
                        val dblTemperature = jsonObjectTwo.getDouble("temp")

                        binding.iconTemp.setAnimation(getWeatherAnimation(strDescWeather))
                        binding.tvWeather.text = getWeatherDescription(strDescWeather)
                        toolbarBinding.tvName.text = strName
                        binding.tvTemperature.text =
                            String.format(Locale.getDefault(), "%.0f°C", dblTemperature)
                        binding.tvWindvelocity.text =
                            "Wind Velocity $strWindvelocity km/j"
                        binding.tvHumidity.text = "Humidity $strHumidity %"
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to display header data!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(
                        this@MainActivity,
                        "No internet network!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun getListWeather() {
        mProgressBar?.show()
        AndroidNetworking.get(
            Apiservice.BASEURL + Apiservice.ListWeather + "lat=" + lat + "&lon=" + lng + Apiservice.UnitsAppid
        )
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        mProgressBar?.dismiss()
                        val jsonArray = response.getJSONArray("list")
                        for (i in 0..6) {
                            val dataApi = ModelMain()
                            val objectList = jsonArray.getJSONObject(i)
                            val jsonObjectOne = objectList.getJSONObject("main")
                            val jsonArrayOne = objectList.getJSONArray("weather")
                            val jsonObjectTwo = jsonArrayOne.getJSONObject(0)
                            var timeNow = objectList.getString("dt_txt")
                            val formatDefault = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val formatTimeCustom = SimpleDateFormat("kk:mm", Locale.getDefault())

                            try {
                                val timesFormat = formatDefault.parse(timeNow)
                                timeNow = formatTimeCustom.format(timesFormat)
                            } catch (e: ParseException) {
                                e.printStackTrace()
                            }

                            dataApi.timeNow = timeNow
                            dataApi.currentTemp = jsonObjectOne.getDouble("temp")
                            dataApi.descWeather = jsonObjectTwo.getString("description")
                            dataApi.tempMin = jsonObjectOne.getDouble("temp_min")
                            dataApi.tempMax = jsonObjectOne.getDouble("temp_max")
                            modelMain.add(dataApi)
                        }
                        mainAdapter?.notifyDataSetChanged()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to display data!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    mProgressBar?.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "No internet network!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun getWeatherAnimation(description: String): Int {
        return when (description) {
            "broken clouds" -> R.raw.broken_clouds
            "light rain" -> R.raw.light_rain
            "haze" -> R.raw.broken_clouds
            "overcast clouds" -> R.raw.overcast_clouds
            "moderate rain" -> R.raw.moderate_rain
            "few clouds" -> R.raw.few_clouds
            "heavy intensity rain" -> R.raw.heavy_intensity
            "clear sky" -> R.raw.clear_sky
            "scattered clouds" -> R.raw.scattered_clouds
            else -> R.raw.unknown
        }
    }

    private fun getWeatherDescription(description: String): String {
        return when (description) {
            "broken clouds" -> "Scattered Clouds"
            "light rain" -> "Drizzling"
            "haze" -> "Foggy"
            "overcast clouds" -> "Overcast Clouds"
            "moderate rain" -> "Light rain"
            "few clouds" -> "Cloudy"
            "heavy intensity rain" -> "Heavy rain"
            "clear sky" -> "Sunny"
            "scattered clouds" -> "Scattered Clouds"
            else -> description
        }
    }

    private fun checkIfAlreadyhavePermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun checkIfAlreadyhavePermission2(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                val intent = intent
                finish()
                startActivity(intent)
            } else {
                getLatlong()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(s: String?, i: Int, bundle: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    companion object {
        fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
            val window = activity.window
            val layoutParams = window.attributes
            if (on) {
                layoutParams.flags = layoutParams.flags or bits
            } else {
                layoutParams.flags = layoutParams.flags and bits.inv()
            }
            window.attributes = layoutParams
        }
    }
}
