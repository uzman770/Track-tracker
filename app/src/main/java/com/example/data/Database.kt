package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey val id: String,
    val name: String,
    val schedule: String, // Comma separated, e.g., "Monday, Thursday"
    val adminId: String,
    val adminName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_status", primaryKeys = ["areaId", "date"])
data class DailyStatus(
    val areaId: String,
    val date: String, // YYYY-MM-DD
    val status: String, // "coming" | "not_coming" | "delayed"
    val note: String?,
    val updatedBy: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val areaId: String,
    val notifyPref: String = "push", // "push" | "sms" | "email"
    val isSubscribed: Boolean = true
)

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val areaId: String,
    val areaName: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Dao
interface AreaDao {
    @Query("SELECT * FROM areas ORDER BY name ASC")
    fun getAllAreasFlow(): Flow<List<Area>>

    @Query("SELECT * FROM areas ORDER BY name ASC")
    suspend fun getAllAreas(): List<Area>

    @Query("SELECT * FROM areas WHERE id = :areaId LIMIT 1")
    suspend fun getAreaById(areaId: String): Area?

    @Query("SELECT * FROM areas WHERE adminId = :adminId ORDER BY name ASC")
    fun getAreasByAdminFlow(adminId: String): Flow<List<Area>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<Area>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: Area)
}

@Dao
interface DailyStatusDao {
    @Query("SELECT * FROM daily_status WHERE areaId = :areaId AND date = :date LIMIT 1")
    fun getDailyStatusFlow(areaId: String, date: String): Flow<DailyStatus?>

    @Query("SELECT * FROM daily_status WHERE areaId = :areaId AND date = :date LIMIT 1")
    suspend fun getDailyStatus(areaId: String, date: String): DailyStatus?

    @Query("SELECT * FROM daily_status WHERE areaId = :areaId ORDER BY date DESC LIMIT 30")
    fun getAreaStatusHistoryFlow(areaId: String): Flow<List<DailyStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStatus(dailyStatus: DailyStatus)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE isSubscribed = 1")
    fun getActiveSubscriptionsFlow(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE areaId = :areaId LIMIT 1")
    suspend fun getSubscription(areaId: String): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE areaId = :areaId")
    suspend fun deleteSubscription(areaId: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLog)

    @Query("UPDATE notification_logs SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notification_logs SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notification_logs")
    suspend fun clearAllNotifications()
}

@Database(
    entities = [Area::class, DailyStatus::class, Subscription::class, NotificationLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun areaDao(): AreaDao
    abstract fun dailyStatusDao(): DailyStatusDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun notificationLogDao(): NotificationLogDao
}
