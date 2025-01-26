package com.weatherapp.weatherapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import com.weatherapp.weatherapp.R
import com.weatherapp.weatherapp.adapter.RvAdapter
import com.weatherapp.weatherapp.data.forecastModels.ForecastData
import com.weatherapp.weatherapp.databinding.ActivityMainBinding
import com.weatherapp.weatherapp.databinding.BottomSheetLayoutBinding
import com.weatherapp.weatherapp.localstorage.ForecastEntity
import com.weatherapp.weatherapp.localstorage.WeatherEntity
import com.weatherapp.weatherapp.localstorage.database.WeatherDatabase
import com.weatherapp.weatherapp.utils.RetrofitInstance
import com.weatherapp.weatherapp.data.forecastModels.Main
import com.weatherapp.weatherapp.data.forecastModels.Weather
import com.weatherapp.weatherapp.data.forecastModels.Clouds
import com.weatherapp.weatherapp.data.forecastModels.Sys
import com.weatherapp.weatherapp.data.forecastModels.Wind
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sheetLayoutBinding: BottomSheetLayoutBinding
    private lateinit var dialog: BottomSheetDialog
    private var city: String = "nairobi"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var database: WeatherDatabase

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
        database = Room.databaseBuilder(
            applicationContext,
            WeatherDatabase::class.java,
            "weather_database"
        ).build()
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
        getForecast()
        sheetLayoutBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, 1, RecyclerView.HORIZONTAL, false)
        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getForecast() {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getForecast(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                val cachedForecast = database.forecastDao().getForecastByCity(city)
                withContext(Dispatchers.Main) {
                    if (cachedForecast.isNotEmpty()) {
                        updateForecastUI(cachedForecast)
                    } else {
                        Toast.makeText(applicationContext, "No offline forecast data available", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            } catch (e: HttpException) {
                val cachedForecast = database.forecastDao().getForecastByCity(city)
                withContext(Dispatchers.Main) {
                    if (cachedForecast.isNotEmpty()) {
                        updateForecastUI(cachedForecast)
                    } else {
                        Toast.makeText(applicationContext, "No offline forecast data available", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val forecastList = data.list.map { forecast ->
                    ForecastEntity(
                        cityName = city,
                        date = forecast.dt_txt,
                        temperature = "${forecast.main.temp.toInt()}°C",
                        description = forecast.weather[0].description,
                        icon = forecast.weather[0].icon
                    )
                }
                // Save data to local database
                database.forecastDao().insertForecast(forecastList)

                withContext(Dispatchers.Main) {
                    updateForecastUI(forecastList)
                }
            }
        }
    }

    private fun updateForecastUI(forecastList: List<ForecastEntity>) {
        val forecastDataList = ArrayList<ForecastData>()
        forecastList.forEach { entity ->
            forecastDataList.add(
                ForecastData(
                    clouds = Clouds(0),
                    dt = 0,
                    dt_txt = entity.date,
                    main = Main(
                        temp = entity.temperature.removeSuffix("°C").toDouble(),
                        feels_like = 0.0,
                        temp_min = 0.0,
                        temp_max = 0.0,
                        pressure = 0,
                        humidity = 0,
                        grnd_level = 0,
                        sea_level = 0,
                        temp_kf = 0.0
                    ),
                    pop = 0.0,
                    sys = Sys(pod = ""),
                    visibility = 0,
                    weather = listOf(
                        Weather(
                            description = entity.description,
                            icon = entity.icon,
                            id = 0,
                            main = ""
                        )
                    ),
                    wind = Wind(
                        speed = 0.0,
                        deg = 0,
                        gust = 0.0
                    )
                )
            )
        }
        val adapter = RvAdapter(forecastDataList)
        sheetLayoutBinding.rvForecast.adapter = adapter
        sheetLayoutBinding.tvSheet.text = "Five days forecast in ${forecastList.firstOrNull()?.cityName}"
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getCurrentWeather(city: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    city,
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                // Fetch from local database if API call fails
                val cachedWeather = database.weatherDao().getWeatherByCity(city)
                withContext(Dispatchers.Main) {
                    if (cachedWeather != null) {
                        updateWeatherUI(cachedWeather)
                    } else {
                        Toast.makeText(applicationContext, "No offline weather data available", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            } catch (e: HttpException) {
                val cachedWeather = database.weatherDao().getWeatherByCity(city)
                withContext(Dispatchers.Main) {
                    if (cachedWeather != null) {
                        updateWeatherUI(cachedWeather)
                    } else {
                        Toast.makeText(applicationContext, "No offline weather data available", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val weatherEntity = WeatherEntity(
                    cityName = city,
                    temperature = "${data.main.temp.toInt()}°C",
                    description = data.weather[0].description,
                    humidity = data.main.humidity,
                    windSpeed = data.wind.speed,
                    icon = data.weather[0].icon,
                    sunrise = data.sys.sunrise.toLong(),
                    sunset = data.sys.sunset.toLong(),
                    updateTime = data.dt.toLong()
                )
                // Save data to local database
                database.weatherDao().insertWeather(weatherEntity)
                withContext(Dispatchers.Main) {
                    updateWeatherUI(weatherEntity)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeatherUI(weather: WeatherEntity) {
        binding.apply {
            tvStatus.text = weather.description
            tvTemp.text = weather.temperature
            tvHumidity.text = "${weather.humidity} %"
            tvWind.text = "${weather.windSpeed} KM/H"
            tvSunrise.text = dateFormatConverter(weather.sunrise)
            tvSunset.text = dateFormatConverter(weather.sunset)
            Picasso.get()
                .load("https://openweathermap.org/img/wn/${weather.icon}@4x.png")
                .into(imgWeather)
        }
    }

    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat(
            "hh:mm a",
            Locale.ENGLISH
        ).format(Date(date * 1000))
    }
}