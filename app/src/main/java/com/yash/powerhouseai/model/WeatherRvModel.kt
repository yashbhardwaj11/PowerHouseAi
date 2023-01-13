package com.yash.powerhouseai.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "weather")
data class WeatherRvModel(
    @PrimaryKey(autoGenerate = true)
    val id : Int,
    val name: String,
    val speed: String,
    val humidity: String,
    val temp: Int,
    val temp_max: String,
    val temp_min: String,
    val icon: String,
    val forecast: String,
    val lastupdatedAt : Long,
    val state : String
)