package com.weatherapp.weatherapp.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.weatherapp.weatherapp.data.forecastModels.ForecastData
import com.weatherapp.weatherapp.databinding.RvItemLayoutBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Adapter for populating the forecast data into a RecyclerView.
 */
class ForecastAdapter(
    private val forecastList: List<ForecastData> // Changed to List for immutability
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    /**
     * ViewHolder class to bind the item layout with its data.
     */
    class ForecastViewHolder(val binding: RvItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Creates a new ViewHolder by inflating the item layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = RvItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ForecastViewHolder(binding)
    }

    /**
     * Binds data to the ViewHolder for the given position.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.binding.apply {
            // Load weather icon from the URL
            val iconCode = forecast.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/w/$iconCode.png"
            Picasso.get().load(iconUrl).into(imgItem)

            // Display temperature and weather description
            tvItemTemp.text = "${forecast.main.temp.toInt()}°C"
            tvItemStatus.text = forecast.weather[0].description.capitalize()

            // Format and display time
            tvItemTime.text = formatDateTime(forecast.dt_txt)
        }
    }

    /**
     * Formats the given datetime string to a more readable format.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDateTime(dateTimeStr: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
        val parsedDateTime = LocalDateTime.parse(dateTimeStr, inputFormatter)
        return outputFormatter.format(parsedDateTime)
    }

    /**
     * Returns the total number of forecast items.
     */
    override fun getItemCount(): Int = forecastList.size
}