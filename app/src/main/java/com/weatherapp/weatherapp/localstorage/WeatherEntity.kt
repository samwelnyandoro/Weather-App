package com.weatherapp.weatherapp.localstorage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_table")
data class WeatherEntity(
    @PrimaryKey val cityName: String,
    val temperature: String,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val icon: String,
    val sunrise: Long,
    val sunset: Long,
    val updateTime: Long
)