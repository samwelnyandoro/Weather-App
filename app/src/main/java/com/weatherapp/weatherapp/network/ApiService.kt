package com.weatherapp.weatherapp.network

import com.weatherapp.weatherapp.data.forecastModels.Forecast
import com.weatherapp.weatherapp.data.currentWeatherModels.CurrentWeather
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("weather?")
    suspend fun getCurrentWeather(
        @Query("q") city : String,
        @Query("units") units : String,
        @Query("appid") apiKey : String,
    ): Response<CurrentWeather>

    @GET("forecast?")
    suspend fun getForecast(
        @Query("q") city: String,
        @Query("units") units : String,
        @Query("appid") apiKey : String,
    ) : Response<Forecast>
}