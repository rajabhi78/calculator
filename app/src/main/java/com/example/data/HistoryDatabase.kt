package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calculations")
data class Calculation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<Calculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: Calculation)

    @Query("DELETE FROM calculations")
    suspend fun clearAllCalculations()

    @Delete
    suspend fun deleteCalculation(calculation: Calculation)
}

@Database(entities = [Calculation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
}

class CalculationRepository(private val dao: CalculationDao) {
    val allCalculations: Flow<List<Calculation>> = dao.getAllCalculations()

    suspend fun insert(expression: String, result: String) {
        if (expression.isNotBlank() && result.isNotBlank()) {
            dao.insertCalculation(Calculation(expression = expression, result = result))
        }
    }

    suspend fun delete(calculation: Calculation) {
        dao.deleteCalculation(calculation)
    }

    suspend fun clearAll() {
        dao.clearAllCalculations()
    }
}
