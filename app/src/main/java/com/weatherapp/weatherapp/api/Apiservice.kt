package com.weatherapp.weatherapp.api

import com.weatherapp.weatherapp.BuildConfig

object Apiservice {
    var BASEURL = BuildConfig.OPENWEATHERMAP_BASEURL
    var CurrentWeather = "weather?"
    var ListWeather = "forecast?"
    var Daily = "forecast/daily?"
    var UnitsAppid = "&units=metric&appid=${BuildConfig.OPENWEATHERMAP_API_KEY}"
    var UnitsAppidDaily = "&units=metric&cnt=15&appid=${BuildConfig.OPENWEATHERMAP_API_KEY}"
}