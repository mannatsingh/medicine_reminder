package com.mannat.medicine_reminder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mannat.medicine_reminder.data.local.db.converter.Converters
import com.mannat.medicine_reminder.data.local.db.dao.DoseLogDao
import com.mannat.medicine_reminder.data.local.db.dao.MedicineDao
import com.mannat.medicine_reminder.data.local.db.dao.ScheduleDao
import com.mannat.medicine_reminder.data.local.db.entity.DoseLogEntity
import com.mannat.medicine_reminder.data.local.db.entity.MedicineEntity
import com.mannat.medicine_reminder.data.local.db.entity.ScheduleEntity

@Database(
    entities = [
        MedicineEntity::class,
        ScheduleEntity::class,
        DoseLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        const val DATABASE_NAME = "medicine_reminder.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN reminderWindowMinutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
