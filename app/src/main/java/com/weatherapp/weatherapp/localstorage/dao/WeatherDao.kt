package com.weatherapp.weatherapp.localstorage.dao

import androidx.room.*
import com.weatherapp.weatherapp.localstorage.WeatherEntity

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather_table WHERE cityName = :cityName")
    suspend fun getWeatherByCity(cityName: String): WeatherEntity?
}