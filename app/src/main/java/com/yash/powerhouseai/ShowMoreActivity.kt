package com.yash.powerhouseai

import android.annotation.SuppressLint
import android.content.Context
import android.icu.util.ULocale.getCountry
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.room.Room
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.yash.powerhouseai.databinding.ActivityShowMoreBinding
import com.yash.powerhouseai.model.WeatherRvModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ShowMoreActivity : AppCompatActivity() {
    private lateinit var binding : ActivityShowMoreBinding
    private  var stateName: String? = null
    private lateinit var api_key : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stateName = intent.getStringExtra("stateName")
        
        api_key = "your_api_key"

        if (isInternetAvailable()){
            getCountryData(stateName)
        }
        else{
            fetchDataForOffline()
            val message = "No internet connection"
            if(message.isNotEmpty()){
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun fetchDataForOffline() {
        val db = Room.databaseBuilder(applicationContext,
            WeatherDatabase::class.java,
            "weather_db_country"
        ).build()
        GlobalScope.launch(Dispatchers.IO) {
            val lastModel = db.weatherDao().getLastCountry(stateName!!)
            withContext(Dispatchers.Main){
                if (lastModel!=null){
                    binddata(lastModel)

                    binding.cardLayout.visibility = View.VISIBLE
                }else{
                    binding.cityName.text = "No data Available"
                    binding.lastUpdated.text = "Connect to a internet connection"
                    binding.cardLayout.visibility = View.INVISIBLE
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun isInternetAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun getCountryData(stateName: String?) {
        when(stateName){
            "Delhi" -> {
                startFillingLocation(stateName,"in")
            }
            "New York" -> {
                startFillingLocation(stateName,"us")
            }
            "Singapore" -> {
                startFillingLocation(stateName,"sg")
            }
            "Mumbai" ->{
                startFillingLocation(stateName,"in")
            }
            "Sydney" -> {
                startFillingLocation(stateName,"au")
            }
            "Melbourne" -> {
                startFillingLocation(stateName,"au")
            }
        }
    }

    private fun startFillingLocation(stateName: String?,countryName : String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$stateName,$countryName&APPID=$api_key"

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,url,null,
            { response ->
//                Toast.makeText(applicationContext, "${response.toString()}", Toast.LENGTH_SHORT).show()
                pushingValues(response)

            }, {
                Toast.makeText(applicationContext, it.printStackTrace().toString(), Toast.LENGTH_SHORT).show()

            })

        requestQueue.add(jsonObjectRequest)
    }

    private fun pushingValues(response: JSONObject?) {
        val name = response?.getString("name")
        binding.cityName.text = name
        val main = response?.getJSONObject("main")
        val temp = main?.getInt("temp")
        val temp_min = main?.getInt("temp_min")
        val temp_max = main?.getInt("temp_max")
        val temp_minV = temp_min as Int
        val temp_min_final = temp_minV-273
        val temp_maxV = temp_max as Int
        val temp_max_final = temp_maxV-273
        val sys = response.getJSONObject("sys")
        val sunrise = sys.getLong("sunrise")
        val sunset = sys.getLong("sunset")
        val humidity = main.getString("humidity")

        val wind = response.getJSONObject("wind")
        val windSpeed = wind.getString("speed")

        val weather = response.getJSONArray("weather").getJSONObject(0)
        val forecast = weather.getString("main")
        val forecastIcon = "http://openweathermap.org/img/wn/${weather.getString("icon")}.png"
        Log.d("forecastIcon", forecastIcon)

        val mainTemp : Int = temp!! - 273

        val date = response.getLong("dt")



        val weatherModel = WeatherRvModel(0,name!!,windSpeed, humidity,mainTemp,temp_max_final.toString(),temp_min_final.toString(),
            forecastIcon,forecast, date,stateName!!)

        binddata(weatherModel)

        val db = Room.databaseBuilder(applicationContext,
            WeatherDatabase::class.java,
            "weather_db_country"
        ).build()
        GlobalScope.launch(Dispatchers.IO) {

            val tempModel = db.weatherDao().getLastCountry(stateName!!)

            if (tempModel?.lastupdatedAt != weatherModel.lastupdatedAt){
                db.weatherDao().insert(weatherModel)
            }
        }
    }

    private fun binddata(weatherModel: WeatherRvModel) {
        val final = longToDate(weatherModel.lastupdatedAt)
        binding.cityName.text = weatherModel.name
        binding.lastUpdated.text = "last updated at $final"
        binding.sunsetValue.text = weatherModel.humidity
        binding.sunriseTime.text = weatherModel.forecast

        binding.windSpeed.text = weatherModel.speed
        binding.currentTemp.text = "${weatherModel.temp}°C"

        binding.minTemp.text = "min temp : ${weatherModel.temp_min.toString()}°C"
        binding.maxTemp.text = "max temp : ${weatherModel.temp_min.toString()}°C"
    }

    private fun longToDate(time: Long): String {
        val format = "dd MMM yyyy HH:mm"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(time * 1000))
    }
}
