package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "patient_assessments")
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val age: Int,
    val isMale: Boolean,
    val overallCadPositive: Boolean,
    val overallRiskPercent: Int,
    val ladStenosisPercent: Int,
    val lcxStenosisPercent: Int,
    val rcaStenosisPercent: Int,
    val troponinI: Float,
    val ejectionFraction: Int,
    val primaryTerritory: String,
    val clinicalSummary: String
)

@Dao
interface AssessmentRecordDao {
    @Query("SELECT * FROM patient_assessments ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AssessmentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AssessmentRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: AssessmentRecordEntity)

    @Query("DELETE FROM patient_assessments")
    suspend fun clearAll()
}

@Database(entities = [AssessmentRecordEntity::class], version = 1, exportSchema = false)
abstract class CardioDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentRecordDao

    companion object {
        @Volatile
        private var INSTANCE: CardioDatabase? = null

        fun getDatabase(context: Context): CardioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardioDatabase::class.java,
                    "cardiovision_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
