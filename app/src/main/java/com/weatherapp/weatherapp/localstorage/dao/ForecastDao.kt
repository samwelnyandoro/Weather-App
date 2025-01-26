package com.weatherapp.weatherapp.localstorage.dao

import androidx.room.*
import com.weatherapp.weatherapp.localstorage.ForecastEntity

@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecastList: List<ForecastEntity>)

    @Query("SELECT * FROM forecast_table WHERE cityName = :cityName")
    suspend fun getForecastByCity(cityName: String): List<ForecastEntity>
}