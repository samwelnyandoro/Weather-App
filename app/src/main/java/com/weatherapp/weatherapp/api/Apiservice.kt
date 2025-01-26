package com.weatherapp.weatherapp.api

import com.weatherapp.weatherapp.BuildConfig

object Apiservice {
    var BASEURL = BuildConfig.OPENWEATHERMAP_BASEURL
    var CurrentWeather = "weather?"
    var ListWeather = "forecast?"
    var Daily = "forecast/daily?"
    var UnitsAppid = "&units=metric&appid=${BuildConfig.OPENWEATHERMAP_API_KEY}"
    var UnitsAppidDaily = "&units=metric&cnt=15&appid=${BuildConfig.OPENWEATHERMAP_API_KEY}"
//var BASEURL = "http://api.openweathermap.org/data/2.5/"
//    var CurrentWeather = "weather?"
//    var ListWeather = "forecast?"
//    var Daily = "forecast/daily?"
//    var UnitsAppid = "&units=metric&appid=cfe577b09f43deea2722462eea76e473"
//    var UnitsAppidDaily = "&units=metric&cnt=15&appid=cfe577b09f43deea2722462eea76e473"
}