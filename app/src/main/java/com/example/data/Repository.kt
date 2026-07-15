package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TrashNotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "garbage_pickup_alerts"
        const val CHANNEL_NAME = "Garbage Pickup Alerts"
        const val CHANNEL_DESC = "Notifications for garbage truck pickup status in subscribed areas."
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPickupNotification(areaName: String, status: String, note: String?) {
        val statusText = when (status) {
            "coming" -> "🚛 Garbage truck is coming today!"
            "not_coming" -> "❌ No garbage pickup today."
            "delayed" -> "⏰ Pickup delayed" + if (!note.isNullOrBlank()) ": $note" else "."
            else -> "Status updated."
        }

        // Use the default app icon as small icon
        val smallIcon = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("Garbage Update: $areaName")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AppRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val areaDao = database.areaDao()
    private val dailyStatusDao = database.dailyStatusDao()
    private val subscriptionDao = database.subscriptionDao()
    private val notificationLogDao = database.notificationLogDao()
    private val notificationHelper = TrashNotificationHelper(context)

    // Flow queries
    val allAreas: Flow<List<Area>> = areaDao.getAllAreasFlow()
    val activeSubscriptions: Flow<List<Subscription>> = subscriptionDao.getActiveSubscriptionsFlow()
    val notificationLogs: Flow<List<NotificationLog>> = notificationLogDao.getAllNotificationsFlow()

    fun getAreasByAdminFlow(adminId: String): Flow<List<Area>> = areaDao.getAreasByAdminFlow(adminId)
    fun getDailyStatusFlow(areaId: String, date: String): Flow<DailyStatus?> = dailyStatusDao.getDailyStatusFlow(areaId, date)
    fun getAreaStatusHistoryFlow(areaId: String): Flow<List<DailyStatus>> = dailyStatusDao.getAreaStatusHistoryFlow(areaId)

    suspend fun getDailyStatus(areaId: String, date: String): DailyStatus? = dailyStatusDao.getDailyStatus(areaId, date)

    // Prepopulate Areas if the database is empty
    suspend fun prepopulateDatabaseIfEmpty() {
        val existingAreas = areaDao.getAllAreas()
        if (existingAreas.isEmpty()) {
            val defaultAreas = listOf(
                Area(
                    id = "area_north_heights",
                    name = "North Heights",
                    schedule = "Monday, Thursday",
                    adminId = "admin_john",
                    adminName = "John Doe"
                ),
                Area(
                    id = "area_westwood_village",
                    name = "Westwood Village",
                    schedule = "Tuesday, Friday",
                    adminId = "admin_jane",
                    adminName = "Jane Smith"
                ),
                Area(
                    id = "area_sunnyvale_gardens",
                    name = "Sunnyvale Gardens",
                    schedule = "Wednesday, Saturday",
                    adminId = "admin_john",
                    adminName = "John Doe"
                ),
                Area(
                    id = "area_downtown_core",
                    name = "Downtown Core",
                    schedule = "Monday, Wednesday, Friday",
                    adminId = "admin_jane",
                    adminName = "Jane Smith"
                )
            )
            areaDao.insertAreas(defaultAreas)

            // Seed some realistic status history
            val statuses = listOf("coming", "not_coming", "delayed")
            val notes = listOf(null, "Arriving after 2pm", "Due to holiday reschedule", null)
            val baseTime = System.currentTimeMillis()
            val dayMillis = 86400000L

            for (area in defaultAreas) {
                // Seed last 4 days
                for (i in 1..4) {
                    val dateStr = getFormattedDateMinusDays(i)
                    val status = statuses[i % statuses.size]
                    val note = if (status == "delayed") notes[i % notes.size] else null
                    dailyStatusDao.insertDailyStatus(
                        DailyStatus(
                            areaId = area.id,
                            date = dateStr,
                            status = status,
                            note = note,
                            updatedBy = area.adminId,
                            updatedAt = baseTime - (i * dayMillis)
                        )
                    )
                }
            }
        }
    }

    private fun getFormattedDateMinusDays(days: Int): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        return formatter.format(calendar.time)
    }

    // Insert new Area (Admin action)
    suspend fun createArea(name: String, schedule: String, adminId: String, adminName: String) {
        val newArea = Area(
            id = "area_" + System.currentTimeMillis(),
            name = name,
            schedule = schedule,
            adminId = adminId,
            adminName = adminName
        )
        areaDao.insertArea(newArea)
    }

    // Set daily status (Admin action)
    suspend fun updateDailyStatus(
        areaId: String,
        date: String,
        status: String,
        note: String?,
        adminId: String
    ) {
        val dailyStatus = DailyStatus(
            areaId = areaId,
            date = date,
            status = status,
            note = note,
            updatedBy = adminId,
            updatedAt = System.currentTimeMillis()
        )
        dailyStatusDao.insertDailyStatus(dailyStatus)

        // Get Area details
        val area = areaDao.getAreaById(areaId) ?: return

        // Check if current user (resident) is subscribed to this area to trigger notifications
        val isSubscribed = subscriptionDao.getSubscription(areaId)?.isSubscribed == true
        if (isSubscribed) {
            val title = "Garbage Update: ${area.name}"
            val statusText = when (status) {
                "coming" -> "🚛 Garbage truck IS coming today!"
                "not_coming" -> "❌ No pickup today."
                "delayed" -> "⏰ Pickup delayed" + if (!note.isNullOrBlank()) ": $note" else "."
                else -> "Status updated."
            }

            // Create notification log in database
            val log = NotificationLog(
                areaId = areaId,
                areaName = area.name,
                title = title,
                body = statusText,
                timestamp = System.currentTimeMillis()
            )
            notificationLogDao.insertNotificationLog(log)

            // Trigger actual system notification
            notificationHelper.showPickupNotification(area.name, status, note)
        }
    }

    // Subscribe / Unsubscribe (Resident Action)
    suspend fun subscribeToArea(areaId: String, isSubscribed: Boolean, notifyPref: String = "push") {
        if (isSubscribed) {
            val subscription = Subscription(
                areaId = areaId,
                notifyPref = notifyPref,
                isSubscribed = true
            )
            subscriptionDao.insertSubscription(subscription)
        } else {
            subscriptionDao.deleteSubscription(areaId)
        }
    }

    suspend fun updateNotificationPref(areaId: String, pref: String) {
        val subscription = Subscription(
            areaId = areaId,
            notifyPref = pref,
            isSubscribed = true
        )
        subscriptionDao.insertSubscription(subscription)
    }

    // Notification Logs Operations
    suspend fun markNotificationAsRead(id: Int) = notificationLogDao.markAsRead(id)
    suspend fun markAllNotificationsAsRead() = notificationLogDao.markAllAsRead()
    suspend fun clearAllNotifications() = notificationLogDao.clearAllNotifications()
}
