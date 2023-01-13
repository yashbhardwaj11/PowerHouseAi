package com.yash.powerhouseai

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yash.powerhouseai.Dao.WeatherDao
import com.yash.powerhouseai.model.WeatherRvModel

@Database(entities = [WeatherRvModel::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao() : WeatherDao
}
