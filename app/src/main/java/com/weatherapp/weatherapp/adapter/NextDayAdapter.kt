package com.weatherapp.weatherapp.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.weatherapp.R
import com.weatherapp.weatherapp.databinding.ListItemNextDaysBinding
import com.weatherapp.weatherapp.model.ModelNextDay
import java.util.*

class NextDayAdapter(
    private val mContext: Context,
    private val items: List<ModelNextDay>
) : RecyclerView.Adapter<NextDayAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemNextDaysBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]

        holder.binding.tvNameDay.text = data.nameDay
        holder.binding.tvDate.text = data.nameDate
        holder.binding.tvMinTemp.text = String.format(Locale.getDefault(), "%.0f°C", data.tempMin)
        holder.binding.tvMaxTemp.text = String.format(Locale.getDefault(), "%.0f°C", data.tempMax)

        // Simplified weather animation logic with 'when' expression
        val animationRes = when (data.descWeather) {
            "broken clouds" -> R.raw.broken_clouds
            "light rain" -> R.raw.light_rain
            "overcast clouds" -> R.raw.overcast_clouds
            "moderate rain" -> R.raw.moderate_rain
            "few clouds" -> R.raw.few_clouds
            "heavy intensity rain" -> R.raw.heavy_intensity
            "clear sky" -> R.raw.clear_sky
            "scattered clouds" -> R.raw.scattered_clouds
            else -> R.raw.unknown
        }
        holder.binding.iconTemp.setAnimation(animationRes)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: ListItemNextDaysBinding) : RecyclerView.ViewHolder(binding.root)
}
