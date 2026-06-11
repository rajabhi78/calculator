package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CalculationRepository

class CalculatorApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "calculator_database"
        ).build()
    }
    val repository by lazy {
        CalculationRepository(database.calculationDao())
    }
}
