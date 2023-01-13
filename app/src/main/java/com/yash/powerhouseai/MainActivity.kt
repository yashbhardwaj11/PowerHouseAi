package com.yash.powerhouseai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*
import com.google.android.gms.location.*
import com.yash.powerhouseai.databinding.ActivityMainBinding
import com.yash.powerhouseai.model.WeatherRvModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var PERMISSION_ID : Int = 42
    private lateinit var showMoreBT : Button
    private lateinit var api_key : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        api_key = "your_api_key"

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        showMoreBT = findViewById(R.id.showMoreBT)

        binding.showMoreBT.setOnClickListener {
            startActivity(Intent(this,ChooseCounty::class.java))
        }


        if (isInternetAvailable()){
            getLastLocation()
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
            "weather_db"
        ).build()
        GlobalScope.launch(Dispatchers.IO) {
            val lastModel = db.weatherDao().getLast()
            withContext(Dispatchers.Main){
                if (lastModel!=null){
                    binddata(lastModel)

                    binding.cardLayout.visibility = View.VISIBLE
                    binding.showMoreBT.visibility = View.VISIBLE
                }else{
                    binding.cityName.text = "No data Available"
                    binding.lastUpdated.text = "Connect to a internet connection"
                    binding.cardLayout.visibility = View.INVISIBLE
                    binding.showMoreBT.visibility = View.INVISIBLE
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

    override fun onStart() {
        super.onStart()
        getLastLocation()
    }



    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }


    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermission()) {
            if (isLocationEnable()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(applicationContext, "Location is null", Toast.LENGTH_SHORT).show()
                        requestNewLocationData()
                    } else {
//                        Toast.makeText(applicationContext, "${location.latitude.toString()}", Toast.LENGTH_SHORT).show()
                        startFillingLocation(location.latitude,location.longitude)
                    }
                }
            } else {
                Toast.makeText(this, "Turn the location on", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null)
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            startFillingLocation(mLastLocation.latitude,mLastLocation.longitude)
        }
    }

    private fun startFillingLocation(latitude: Double, longitude: Double) {
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$api_key"

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET,url,null,
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
        forecastIcon,forecast, date,"current")

        binddata(weatherModel)

        val db = Room.databaseBuilder(applicationContext,
            WeatherDatabase::class.java,
            "weather_db"
        ).build()
        GlobalScope.launch(Dispatchers.IO) {

            val tempModel = db.weatherDao().getLast()

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
