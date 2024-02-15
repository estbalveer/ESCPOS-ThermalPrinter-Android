package com.dantsu.thermalprinter

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppPreference(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveIp(ip: String) {
        val list = getIpList()
        list.add(ip)
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("ips", json).apply()
    }

    fun getIpList(): ArrayList<String> {
        val json = sharedPreferences.getString("ips", null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<ArrayList<String>>() {}.type)
        } else {
            ArrayList()
        }
    }
}
