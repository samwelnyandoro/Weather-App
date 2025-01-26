package com.weatherapp.weatherapp.localstorage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecast_table")
data class ForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val date: String,
    val temperature: String,
    val description: String,
    val icon: String
)