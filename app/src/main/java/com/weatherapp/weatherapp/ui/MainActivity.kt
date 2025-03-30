@file:Suppress("DEPRECATION")

package com.weatherapp.weatherapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.weatherapp.weatherapp.R
import com.weatherapp.weatherapp.adapter.ForecastAdapter
import com.weatherapp.weatherapp.data.forecastModels.Forecast
import com.weatherapp.weatherapp.data.forecastModels.ForecastData
import com.weatherapp.weatherapp.data.currentWeatherModels.CurrentWeather
import com.weatherapp.weatherapp.databinding.ActivityMainBinding
import com.weatherapp.weatherapp.databinding.BottomSheetLayoutBinding
import com.weatherapp.weatherapp.network.RetrofitInstance
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sheetLayoutBinding: BottomSheetLayoutBinding
    private lateinit var dialog: BottomSheetDialog
    private var city: String = "nairobi"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sheetLayoutBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetLayoutBinding.root)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query!= null){
                    city = query
                }
                getCurrentWeather(city)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        getCurrentWeather(city)
        binding.tvForecast.setOnClickListener {
            openDialog()
        }
        binding.tvLocation.setOnClickListener {
            fetchLocation()
        }
    }
    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),101
            )
            return
        }
        task.addOnSuccessListener {
            val geocoder=Geocoder(this,Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                geocoder.getFromLocation(it.latitude,it.longitude,1, object: Geocoder.GeocodeListener{
                    override fun onGeocode(addresses: MutableList<Address>) {
                        city = addresses[0].locality
                    }
                })
            }else{
                val address = geocoder.getFromLocation(it.latitude,it.longitude,1) as List<Address>
                city = address[0].locality
            }
            getCurrentWeather(city)
        }
    }

    private fun openDialog() {
        if (dialog.isShowing) {
            return
        }
        getForecast(city)
        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, 1, RecyclerView.HORIZONTAL, false)
        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getForecast(city: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isNetworkAvailable()) {
                // Network is available, try fetching data from the API
                val response = try {
                    RetrofitInstance.api.getForecast(
                        city,
                        "metric",
                        applicationContext.getString(R.string.api_key)
                    )
                } catch (e: IOException) {
                    // Network error, load cached data silently
                    loadCachedForecast(city)
                    return@launch
                } catch (e: HttpException) {
                    // HTTP error, load cached data silently
                    loadCachedForecast(city)
                    return@launch
                }
                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.Main) {
                        val data = response.body()!!
                        saveToLocalStorage("forecast_$city", data) // Save forecast data for specific city
                        val forecastArray = data.list as ArrayList<ForecastData>
                        val adapter = ForecastAdapter(forecastArray)
                        sheetLayoutBinding.rvForecast.adapter = adapter
                        sheetLayoutBinding.tvSheet.text = "Five days forecast in ${data.city.name}"
                    }
                } else {
                    // Response is unsuccessful, load cached data silently
                    loadCachedForecast(city)
                }
            } else {
                // No network available, immediately fallback to cached data
                loadCachedForecast(city)
            }
        }
    }

    private suspend fun loadCachedForecast(city: String) {
        val cachedData = getFromLocalStorage("forecast_$city", Forecast::class.java)
        if (cachedData != null) {
            withContext(Dispatchers.Main) {
                // Show dialog with cached data
                if (!dialog.isShowing) {
                    openDialog() // Open the dialog
                }
                val forecastArray = cachedData.list as ArrayList<ForecastData>
                val adapter = ForecastAdapter(forecastArray)
                sheetLayoutBinding.rvForecast.adapter = adapter
                sheetLayoutBinding.tvSheet.text = "Five days forecast in ${cachedData.city.name}"
            }
        } else {
            withContext(Dispatchers.Main) {
                // still show a message if cached data is unavailable
                showToast("No cached forecast data available.")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getCurrentWeather(city: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isNetworkAvailable()) {
                // Network is available, try fetching data from the API
                val response = try {
                    RetrofitInstance.api.getCurrentWeather(
                        city,
                        "metric",
                        applicationContext.getString(R.string.api_key)
                    )
                } catch (e: IOException) {
                    // Network error, load cached data silently
                    loadCachedWeather(city)
                    return@launch
                } catch (e: HttpException) {
                    // HTTP error, load cached data silently
                    loadCachedWeather(city)
                    return@launch
                }
                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.Main) {
                        val data = response.body()!!
                        saveToLocalStorage("current_weather_$city", data) // Save data for specific city
                        // Update UI
                        val iconId = data.weather[0].icon
                        val imgUrl = "https://openweathermap.org/img/wn/$iconId@4x.png"
                        Picasso.get().load(imgUrl).into(binding.imgWeather)
                        binding.tvSunset.text = dateFormatConverter(data.sys.sunset.toLong())
                        binding.tvSunrise.text = dateFormatConverter(data.sys.sunrise.toLong())
                        binding.apply {
                            tvStatus.text = data.weather[0].description
                            tvWind.text = "${data.wind.speed} KM/H"
                            tvLocation.text = "${data.name}\n${data.sys.country}"
                            tvTemp.text = "${data.main.temp.toInt()}°C"
                            tvFeelsLike.text = getString(R.string.feels_likee, data.main.feels_like.toInt())
                            tvMinTemp.text = getString(R.string.min_temp_show, data.main.temp_min.toInt())  // Convert min temp to Int
                            tvMaxTemp.text = getString(R.string.max_temp_show, data.main.temp_max.toInt())
                            tvHumidity.text = "${data.main.humidity} %"
                            tvRealfeel.text = "${data.main.feels_like.toInt()}°C"
                            tvPressure.text = "${data.main.pressure} hPa"
                            tvUpdateTime.text = getString(R.string.last_update, dateFormatConverter(data.dt.toLong()))
                        }
                    }
                } else {
                    // API response failed, fallback to cached data silently
                    loadCachedWeather(city)
                }
            } else {
                // No network available, immediately fallback to cached data
                loadCachedWeather(city)
            }
        }
    }

    private suspend fun loadCachedWeather(city: String) {
        val cachedData = getFromLocalStorage("current_weather_$city", CurrentWeather::class.java)
        if (cachedData != null) {
            withContext(Dispatchers.Main) {
                // Update UI using cached data
                binding.tvSunset.text = dateFormatConverter(cachedData.sys.sunset.toLong())
                binding.tvSunrise.text = dateFormatConverter(cachedData.sys.sunrise.toLong())
                binding.apply {
                    tvStatus.text = cachedData.weather[0].description
                    tvWind.text = getString(R.string.wind_speedd, cachedData.wind.speed)
                    tvLocation.text = getString(R.string.locationdata, cachedData.name, cachedData.sys.country)
                    tvTemp.text = getString(R.string.temperature, cachedData.main.temp.toInt())
                    tvFeelsLike.text = getString(R.string.feels_like_desc, cachedData.main.feels_like.toInt())
                    tvMinTemp.text = getString(R.string.min_temp_show, cachedData.main.temp_min.toInt())
                    tvMaxTemp.text = getString(R.string.max_temp_show, cachedData.main.temp_max.toInt())
                    tvHumidity.text = getString(R.string.humidity_show, cachedData.main.humidity)
                    tvRealfeel.text = getString(R.string.real_feel, cachedData.main.feels_like.toInt())
                    tvPressure.text = getString(R.string.pressure_show, cachedData.main.pressure)
                    tvUpdateTime.text = getString(R.string.last_update, dateFormatConverter(cachedData.dt.toLong()))
                }
            }
        } else {
            // show a message if cached data is unavailable
            withContext(Dispatchers.Main) {
                showToast("No cached weather data available.")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private val sharedPreferences by lazy {
        getSharedPreferences("weather_app", Context.MODE_PRIVATE)
    }

    private fun saveToLocalStorage(key: String, data: Any) {
        val json = Gson().toJson(data)
        sharedPreferences.edit { putString(key, json) }
    }

    private fun <T> getFromLocalStorage(key: String, classType: Class<T>): T? {
        val json = sharedPreferences.getString(key, null)
        return if (json != null) Gson().fromJson(json, classType) else null
    }

    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat(
            "hh:mm a",
            Locale.ENGLISH
        ).format(Date(date * 1000))
    }
}