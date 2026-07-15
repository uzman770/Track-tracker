package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.Area
import com.example.data.DailyStatus
import com.example.data.NotificationLog
import com.example.data.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AppScreen {
    object RoleSelection : AppScreen()
    object ResidentOnboarding : AppScreen()
    object ResidentHome : AppScreen()
    object ResidentSettings : AppScreen()
    object AdminHome : AppScreen()
    object NotificationTray : AppScreen()
    data class AdminHistory(val areaId: String) : AppScreen()
}

data class AreaWithStatus(
    val area: Area,
    val isSubscribed: Boolean,
    val notifyPref: String,
    val todayStatus: DailyStatus?
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "garbage_notifier_database"
    ).fallbackToDestructiveMigration().build()

    val repository = AppRepository(db, application)

    // Current app screens & navigation stack (in-memory)
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.RoleSelection)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<AppScreen>()

    // Current Role State
    private val _currentRole = MutableStateFlow<String>("unselected") // "resident" | "admin" | "unselected"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Current Logged-in Admin Info
    private val _currentAdminId = MutableStateFlow<String>("admin_john") // default simulation
    val currentAdminId: StateFlow<String> = _currentAdminId.asStateFlow()

    private val _currentAdminName = MutableStateFlow<String>("John Doe")
    val currentAdminName: StateFlow<String> = _currentAdminName.asStateFlow()

    // Search queries for onboarding & dashboard
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Today's Date String: YYYY-MM-DD
    val todayDateString: String = SimpleDateFormat("yyyy-MM-DD", Locale.US).format(Date())
    val todayFormattedDisplay: String = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())

    // Flow of all areas with their subscription status and today's pickup status
    val areasWithStatusList: StateFlow<List<AreaWithStatus>> = combine(
        repository.allAreas,
        repository.activeSubscriptions,
        _searchQuery
    ) { areas, subscriptions, query ->
        val subMap = subscriptions.associateBy { it.areaId }
        val list = mutableListOf<AreaWithStatus>()

        for (area in areas) {
            // Get today's status
            val status = repository.getDailyStatus(area.id, fetchTodayDateString())
            val sub = subMap[area.id]
            val isSubscribed = sub != null
            val notifyPref = sub?.notifyPref ?: "push"

            if (query.isEmpty() || area.name.contains(query, ignoreCase = true)) {
                list.add(AreaWithStatus(area, isSubscribed, notifyPref, status))
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // List of active notification logs
    val notificationLogs: StateFlow<List<NotificationLog>> = repository.notificationLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Populate areas on first-start
            repository.prepopulateDatabaseIfEmpty()

            // Check if user already has subscriptions; if yes, we can auto-route to Resident Home if they choose resident
            // Otherwise, we keep them in onboarding.
        }
    }

    private fun fetchTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Navigation Helper
    fun navigateTo(screen: AppScreen) {
        _screenHistory.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeAt(_screenHistory.size - 1)
        } else {
            _currentScreen.value = AppScreen.RoleSelection
        }
    }

    // Role management
    fun selectRole(role: String, adminId: String = "admin_john", adminName: String = "John Doe") {
        _currentRole.value = role
        if (role == "admin") {
            _currentAdminId.value = adminId
            _currentAdminName.value = adminName
            navigateTo(AppScreen.AdminHome)
        } else if (role == "resident") {
            // Check subscriptions to decide if onboarding is needed
            viewModelScope.launch {
                val subs = repository.activeSubscriptions.first()
                if (subs.isEmpty()) {
                    navigateTo(AppScreen.ResidentOnboarding)
                } else {
                    navigateTo(AppScreen.ResidentHome)
                }
            }
        }
    }

    // Subscription toggles
    fun toggleSubscription(areaId: String, isSubscribed: Boolean) {
        viewModelScope.launch {
            repository.subscribeToArea(areaId, isSubscribed)
        }
    }

    fun updateNotifyPref(areaId: String, pref: String) {
        viewModelScope.launch {
            repository.updateNotificationPref(areaId, pref)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Admin status update action
    fun setDailyStatus(areaId: String, status: String, note: String?) {
        viewModelScope.launch {
            repository.updateDailyStatus(
                areaId = areaId,
                date = fetchTodayDateString(),
                status = status,
                note = note,
                adminId = _currentAdminId.value
            )
        }
    }

    // Admin creates custom residential area
    fun createCustomArea(name: String, schedule: String) {
        viewModelScope.launch {
            repository.createArea(
                name = name,
                schedule = schedule,
                adminId = _currentAdminId.value,
                adminName = _currentAdminName.value
            )
        }
    }

    // Notification operations
    fun markAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    // Fetch history for a specific area as Flow
    fun getStatusHistoryFlow(areaId: String): Flow<List<DailyStatus>> {
        return repository.getAreaStatusHistoryFlow(areaId)
    }
}
