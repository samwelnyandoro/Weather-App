package com.weatherapp.weatherapp.localstorage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.weatherapp.weatherapp.localstorage.ForecastEntity
import com.weatherapp.weatherapp.localstorage.WeatherEntity
import com.weatherapp.weatherapp.localstorage.dao.ForecastDao
import com.weatherapp.weatherapp.localstorage.dao.WeatherDao

@Database(entities = [WeatherEntity::class, ForecastEntity::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun forecastDao(): ForecastDao
}