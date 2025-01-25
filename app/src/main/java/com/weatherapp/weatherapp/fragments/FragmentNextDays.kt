@file:Suppress("DEPRECATION")

package com.weatherapp.weatherapp.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherapp.weatherapp.adapter.NextDayAdapter
import com.weatherapp.weatherapp.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.weatherapp.weatherapp.api.Apiservice
import com.weatherapp.weatherapp.model.ModelNextDay
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FragmentNextDays : BottomSheetDialogFragment(), LocationListener {

    var lat: Double? = null
    var lng: Double? = null
    var nextDayAdapter: NextDayAdapter? = null
    var shimmerLayout: ShimmerFrameLayout? = null
    var rvListWeather: RecyclerView? = null
    var fabClose: FloatingActionButton? = null
    var modelNextDays: MutableList<ModelNextDay> = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as View).setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_next_day, container, false)
        shimmerLayout = rootView.findViewById(R.id.shimmerLayout)
        rvListWeather = rootView.findViewById(R.id.rvListWeather)
        fabClose = rootView.findViewById(R.id.fabClose)
        nextDayAdapter = NextDayAdapter(requireActivity(), modelNextDays)
        rvListWeather?.layoutManager = LinearLayoutManager(activity)
        rvListWeather?.setHasFixedSize(true)
        rvListWeather?.adapter = nextDayAdapter
        shimmerLayout?.startShimmer()
        fabClose?.setOnClickListener {
            dismiss()
        }
        getLatLong()
        return rootView
    }

    @SuppressLint("MissingPermission")
    private fun getLatLong() {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)
        val location = locationManager.getLastKnownLocation(provider.toString())
        if (location != null) {
            onLocationChanged(location)
        } else {
            locationManager.requestLocationUpdates(provider.toString(), 20000, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        lng = location.longitude
        lat = location.latitude
        Handler(Looper.getMainLooper()).postDelayed({
            getListWeather()
        }, 3000)
    }

    private fun getListWeather() {
        AndroidNetworking.get(Apiservice.BASEURL + Apiservice.Daily + "lat=" + lat + "&lon=" + lng + Apiservice.UnitsAppidDaily)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                @SuppressLint("SimpleDateFormat")
                override fun onResponse(response: JSONObject) {
                    try {
                        val jsonArray = response.getJSONArray("list")
                        for (i in 0 until jsonArray.length()) {
                            val dataApi = ModelNextDay()
                            val objectList = jsonArray.getJSONObject(i)
                            val jsonObjectOne = objectList.getJSONObject("temp")
                            val jsonArrayOne = objectList.getJSONArray("weather")
                            val jsonObjectTwo = jsonArrayOne.getJSONObject(0)
                            val longDate = objectList.optLong("dt")
                            val formatDate = SimpleDateFormat("d MMM yy")
                            val readableDate = formatDate.format(Date(longDate * 1000))
                            val longDay = objectList.optLong("dt")
                            val format = SimpleDateFormat("EEEE")
                            val readableDay = format.format(Date(longDay * 1000))

                            dataApi.nameDate = readableDate
                            dataApi.nameDay = readableDay
                            dataApi.descWeather = jsonObjectTwo.getString("description")
                            dataApi.tempMin = jsonObjectOne.getDouble("min")
                            dataApi.tempMax = jsonObjectOne.getDouble("max")
                            modelNextDays.add(dataApi)
                        }
                        nextDayAdapter?.notifyDataSetChanged()
                        shimmerLayout?.stopShimmer()
                        shimmerLayout?.visibility = View.GONE
                        rvListWeather?.visibility = View.VISIBLE
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(activity, "Failed to display data!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(activity, "No internet network!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}

    companion object {
        fun newInstance(string: String?): FragmentNextDays {
            val fragmentNextDays = FragmentNextDays()
            val args = Bundle()
            args.putString("string", string)
            fragmentNextDays.arguments = args
            return fragmentNextDays
        }
    }
}