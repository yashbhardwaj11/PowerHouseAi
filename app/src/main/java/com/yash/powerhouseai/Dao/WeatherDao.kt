package com.yash.powerhouseai.Dao

import androidx.room.*
import com.yash.powerhouseai.model.WeatherRvModel

@Dao
interface WeatherDao{
    @Insert
    fun insert(weatherRvModel: WeatherRvModel)

    @Delete
    fun delete(weatherRvModel: WeatherRvModel)

    @Update
    fun update(weatherRvModel: WeatherRvModel)

    @Query("SELECT * FROM weather WHERE state= :state")
    fun getAll(state : String) : List<WeatherRvModel>

    @Query("SELECT * FROM weather ORDER BY id DESC LIMIT 1")
    fun getLast() : WeatherRvModel?

    @Query("SELECT * FROM weather WHERE state = :stateName  ORDER BY id DESC LIMIT 1")
    fun getLastCountry(stateName : String) : WeatherRvModel?

}

