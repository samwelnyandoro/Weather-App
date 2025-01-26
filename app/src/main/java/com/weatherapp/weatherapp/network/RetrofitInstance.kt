package com.weatherapp.weatherapp.network

import com.weatherapp.weatherapp.data.ApiInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: ApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl(Util.BASEURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}