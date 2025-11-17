# Deadliner Android App - Architecture Documentation

## Project Overview
Deadliner is a feature-rich Android deadline and habit tracking application (v3.1.3). It combines traditional task management with habit tracking capabilities, featuring AI-assisted task creation, cloud synchronization, widgets, and comprehensive analytics.

**Package:** `com.aritxonly.deadliner`  
**Min SDK:** 31 | **Target SDK:** 35 | **Build:** Kotlin + Compose + Material Design 3

---

## 1. Main Source Code Organization

```
app/src/main/java/com/aritxonly/deadliner/
├── data/                           # Data layer (Repository, ViewModel, Database)
│   ├── DatabaseHelper.kt           # SQLite helper (singleton pattern)
│   ├── DDLRepository.kt            # Repository pattern with sync debouncing
│   ├── MainViewModel.kt            # Main MVVM ViewModel
│   └── ViewModelFactory.kt         # Factory for ViewModel creation
├── model/                          # Data models
│   ├── DDLItem.kt                  # Core deadline/habit item (Parcelable)
│   ├── DeadlineType.kt             # Enum: TASK, HABIT
│   ├── HabitMetaData.kt            # Habit-specific metadata
│   ├── AppColorScheme.kt           # Theme color model
│   ├── SyncModel.kt                # Cloud sync data structures
│   ├── AIModel.kt                  # AI request/response models
│   ├── CalendarEvent.kt            # Calendar integration
│   └── PartyPresets.kt             # Celebration animation presets
├── composable/                     # Compose UI screens
│   ├── settings/                   # Settings screens (20+ composables)
│   │   ├── MainSettings.kt
│   │   ├── GeneralSettings.kt
│   │   ├── NotificationSettings.kt
│   │   ├── AISettings.kt
│   │   ├── WebSettings.kt          # Cloud sync config
│   │   ├── BackupSettings.kt
│   │   ├── WidgetSettings.kt
│   │   └── ...
│   ├── overview/                   # Dashboard & analytics
│   │   ├── DashboardScreen.kt      # Main stats overview
│   │   ├── OverviewCharts.kt       # Chart visualizations
│   │   ├── TrendAnalysisScreen.kt  # Trend analysis
│   │   └── OverviewStatsScreen.kt
│   ├── agent/                      # AI overlay
│   │   └── AIOverlay.kt            # Composable AI assistant interface
│   ├── Effects.kt                  # Compose side effects utilities
│   └── SettingsComponents.kt       # Reusable Compose components
├── notification/                   # Notification & alarm system
│   ├── DeadlineAlarmScheduler.kt   # AlarmManager scheduling
│   ├── DeadlineAlarmReceiver.kt    # Alarm broadcast receiver
│   ├── DailyAlarmReceiver.kt       # Daily check-in receiver
│   ├── NotificationUtils.kt        # Notification creation
│   ├── BootReceiver.kt             # Boot completion receiver
│   └── NotificationActionReceiver.kt
├── sync/                           # Cloud synchronization
│   ├── SyncService.kt              # Core sync logic (WebDAV snapshot)
│   ├── SyncWorker.kt               # WorkManager task
│   └── SyncScheduler.kt            # Periodic sync scheduling
├── web/                            # Network & web services
│   ├── AIUtils.kt                  # DeepSeek API client
│   ├── WebUtils.kt                 # WebDAV client
│   ├── UpdateManager.kt            # App update checking
│   └── ApkDownloadInstaller.kt     # Update installation
├── widgets/                        # App widgets
│   ├── MultiDeadlineWidget.kt      # Multi-deadline display widget
│   ├── LargeDeadlineWidget.kt      # Large widget variant
│   ├── HabitMiniWidget.kt          # Mini habit widget
│   ├── HabitMediumWidget.kt        # Medium habit widget
│   └── HabitWidgetConfigureActivity.kt
├── localutils/                     # Utility functions
│   ├── GlobalUtils.kt              # Central config & utilities
│   ├── KeystorePreferencesManager.kt # Secure credential storage
│   ├── OverviewUtils.kt            # Analytics calculations
│   ├── DeadlinerAIConfig.kt        # AI model configuration
│   └── EdgeToEdge.kt               # Edge-to-edge UI utilities
├── ui/theme/                       # Material Design 3 theme
│   ├── Color.kt                    # Color palette
│   ├── Theme.kt                    # Theme composition
│   └── Type.kt                     # Typography
├── intro/                          # Onboarding
│   ├── IntroActivity.kt
│   └── IntroFragmentWelcome.kt
├── calendar/                       # Calendar integration
│   └── CalendarHelper.kt
├── MainActivity.kt                 # Main list view (1800+ lines)
├── DeadlineDetailActivity.kt       # Detail view (Compose)
├── AddDDLActivity.kt               # Creation view
├── EditDDLFragment.kt              # Edit dialog fragment
├── SettingsActivity.kt             # Settings container (Compose NavHost)
├── OverviewActivity.kt             # Analytics view (Compose)
├── ArchiveActivity.kt              # Archived items view
├── LauncherActivity.kt             # Splash/launcher
├── DeadlinerApp.kt                 # Application class (initialization)
├── AppSingletons.kt                # Singleton service locator
├── SearchFilter.kt                 # Search filter parser
└── CustomAdapter.kt                # RecyclerView adapter
```

---

## 2. Core Architectural Patterns

### 2.1 MVVM Architecture
- **ViewModel**: `MainViewModel` manages UI state and data
- **Repository**: `DDLRepository` abstracts data access with debounced sync
- **LiveData/StateFlow**: Used for reactive data binding
- **View**: Activities (traditional + Compose) consume ViewModel data

### 2.2 Repository Pattern
`DDLRepository` wraps database operations and synchronizes changes:
```kotlin
fun insertDDL(...): Long {
    val id = db.insertDDL(...)      // Local insert
    sync.onLocalInserted(id)         // Sync signal
    scheduleSync()                   // 800ms debounced sync
    return id
}
```

### 2.3 Singleton Pattern
- **AppSingletons** provides centralized access to:
  - `DatabaseHelper` (lazy SQLite)
  - `SyncService` (cloud sync)
  - `WebUtils` (HTTP client)

### 2.4 Service Locator Pattern
`AppSingletons.kt` centralizes dependency injection without DI framework.

### 2.5 Adapter Pattern
- `CustomAdapter`: RecyclerView adapter with swipe, multi-select, and check-in listeners
- `IntroViewPagerAdapter`: ViewPager2 adapter for onboarding

### 2.6 Observer Pattern
- LiveData for reactive data updates
- StateFlow for async refresh states
- Broadcast receivers for system events (boot, alarms)

---

## 3. Key Components & Relationships

### 3.1 Database Layer

**DatabaseHelper** (SQLite with version 11):
- Singleton with thread-safe lazy initialization
- Manages `ddl_items` table with soft-delete (tombstone-based)
- Versioning system: `ver_ts`, `ver_ctr`, `ver_dev` (Hybrid Logical Clock)
- UID format: `{deviceId}:{localId}` for cross-device sync

**Core Table Schema:**
```sql
CREATE TABLE ddl_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    start_time, end_time TEXT,
    is_completed, is_archived, is_stared INTEGER,
    note TEXT,                          -- JSON for habit metadata
    type TEXT (task|habit),
    habit_count, habit_total_count INTEGER,
    calendar_event INTEGER,
    deleted INTEGER (soft-delete),
    uid TEXT UNIQUE,
    ver_ts, ver_ctr, ver_dev (versioning)
)
```

**DDLRepository** provides:
- Insert/Update/Delete operations (with sync triggers)
- 800ms debounced sync to prevent network floods
- Direct read-through to DatabaseHelper

### 3.2 Model Layer

**DDLItem** (Parcelable):
- Represents both tasks and habits
- Habit metadata stored as JSON in `note` field:
  ```json
  {
    "frequencyType": "DAILY|WEEKLY|MONTHLY|TOTAL",
    "frequency": 7,
    "completedDates": ["2024-01-01", ...]
  }
  ```

**DeadlineType**:
- `TASK`: One-time deadline
- `HABIT`: Recurring habit with check-in tracking

### 3.3 UI Layer - Hybrid Architecture

**Traditional View-Based (MainActivity)**:
- RecyclerView with `CustomAdapter`
- BottomAppBar with Material Design 3
- Swipe gestures (left=delete, right=complete)
- Multi-select mode with bulk operations
- Search overlay with filters
- Tab-based type switching (Task/Habit)
- Auto-refresh every 30 seconds

**Compose-Based Screens**:
- `SettingsActivity`: Full Compose NavHost with 20+ routes
- `DeadlineDetailActivity`: Item details in Compose
- `OverviewActivity`: Analytics dashboard
- `AIOverlay`: AI assistant overlay (Compose)
- `AddDDLActivity`: Task creation

**Hybrid Approach**:
- MainActivity embeds Compose view for AI overlay
- Activities transition between traditional and Compose
- Shared color scheme via `AppColorScheme` data class (workaround for dynamic colors)

### 3.4 Data Flow

```
User Action
    ↓
Activity/Composable
    ↓
ViewModel.loadData() / filterData()
    ↓
DDLRepository (insert/update/delete)
    ↓
DatabaseHelper.execute()
    ↓
SQLite Database
    ↓
Sync triggers:
  - sync.onLocalInserted(id)
  - sync.onLocalUpdated(id)
  - sync.onLocalDeleting(id)
    ↓
scheduleSync() [800ms debounce]
    ↓
SyncService.syncOnce()
    ↓
WebDAV (snapshot sync)
```

**State Updates:**
```
MainActivity observes:
  - viewModel.ddlList (LiveData<List<DDLItem>>)
  - viewModel.dueSoonCounts (LiveData<Map<Type, Int>>)
  - viewModel.refreshState (StateFlow<RefreshState>)
```

---

## 4. Background Work & Notifications

### 4.1 Notification System

**DeadlineAlarmScheduler**:
- Uses `AlarmManager.setAlarmClock()` for exact alarms
- Schedules notification `hours` before deadline (configurable, default 12h)
- Requires `SCHEDULE_EXACT_ALARM` permission
- Checks if device ignores battery optimization

**Broadcast Receivers**:
- `DeadlineAlarmReceiver`: Triggered by alarm intent
- `DailyAlarmReceiver`: Daily check-in reminders
- `BootReceiver`: Restores alarms after device reboot
- `NotificationActionReceiver`: Handles notification actions

**NotificationUtils**:
- Creates notification channels (Android 8+)
- Supports action buttons (Mark Complete, Snooze)
- Uses Material Design notification styling

### 4.2 WorkManager (Cloud Sync)

**SyncWorker** (CoroutineWorker):
- Periodic background task for cloud synchronization
- Scheduled by `SyncScheduler`
- Validates WebDAV credentials before syncing
- Retries on failure

**SyncScheduler**:
- Enqueues periodic work requests (flexible interval)
- Can be cancelled via settings

### 4.3 Cloud Synchronization (WebDAV)

**SyncService**:
- Implements snapshot-based sync strategy
- Uses HLC (Hybrid Logical Clock) versioning
- Stores local snapshot in `Deadliner/snapshot-v1.json` on WebDAV

**Sync Flow**:
1. Build local snapshot (all non-deleted items + tombstones)
2. Upload to WebDAV
3. Download remote snapshot if newer
4. Merge changes (version comparison)
5. Update local database

**Conflict Resolution**:
- Uses `(ver_ts, ver_ctr, ver_dev)` tuple for ordering
- Last-write-wins strategy
- Tombstones (soft-delete) prevent re-creation of deleted items

---

## 5. Navigation Structure

### Traditional Activities:
```
LauncherActivity (splash)
    ↓
MainActivity (main hub)
    ├─→ AddDDLActivity (create)
    ├─→ DeadlineDetailActivity (detail view)
    ├─→ OverviewActivity (analytics)
    ├─→ ArchiveActivity (archived items)
    └─→ SettingsActivity (settings)

IntroActivity (onboarding)
```

### Compose Navigation (SettingsActivity):
```
NavHost with routes:
  - main
  - general
  - notification
  - interface
  - badge
  - widget
  - ai
  - web
  - backup
  - archive
  - update
  - license
  - policy
  - about
  - feedback
  - donate
  - lab
  - wiki
  - prompt
  - model
```

### Split-View Support (Tablets):
- `DeadlinerApp` configures window embedding rules
- Dynamic split attributes with draggable divider (SDK 31+)
- `MainActivity` + `DeadlineDetailActivity` split pairing

---

## 6. Configuration Files & Purposes

### Build Configuration
- **build.gradle.kts**: Kotlin DSL, Android 35 target, Compose enabled
- **settings.gradle.kts**: Single-module project
- **gradle.properties**: Build configuration

### Android Manifest (AndroidManifest.xml)
- 10+ permissions (notifications, alarms, calendar, internet, battery optimization)
- Activities: Main, Detail, Settings, Overview, Archive, Add, Launcher, Intro
- Receivers: 7 broadcast receivers (alarms, widgets, boot)
- Providers: FileProvider for APK downloads
- Widgets: 4 app widget definitions
- Quick Settings Tile: AddDDLTileService

### Theme & Resources
- **Color.kt**: Material Design 3 color palette (dynamic colors aware)
- **Theme.kt**: DeadlinerTheme composable (light/dark modes)
- **Type.kt**: Typography scales

### Security Configuration
- **network_security_config.xml**: HTTPS/WebDAV configuration
- **data_extraction_rules.xml**: Backup/restore policies

---

## 7. Custom & Noteworthy Patterns

### 7.1 Hybrid View System
**Problem**: Dynamic Material colors don't work in Compose when embedded in traditional View Activity  
**Solution**: Extract theme colors in MainActivity, pass via `AppColorScheme` data class

```kotlin
val materialColorScheme = AppColorScheme(
    primary = getThemeColor(colorPrimary),
    onPrimary = getMaterialThemeColor(colorOnPrimary),
    // ... all colors
)

val intent = DeadlineDetailActivity.newIntent(this, item).apply {
    putExtra("EXTRA_APP_COLOR_SCHEME", materialColorScheme)
}
```

### 7.2 Clipboard-Based AI Task Creation
- Monitors clipboard changes via `ClipboardManager.OnPrimaryClipChangedListener`
- Shows snackbar prompt: "Add from clipboard?" when text changes
- Triggers `AIOverlay` for processing

### 7.3 Habit Check-in System
- JSON-based completion tracking in `note` field
- Supports retroactive check-in with date picker
- Handles frequency-based counting (daily/weekly/monthly/total)

### 7.4 Soft-Delete with Tombstones
- Deleted items kept in database with `deleted=1`
- Enables sync to replicate deletions across devices
- Garbage collection can happen after sync confirmation

### 7.5 Multi-Select with Bulk Operations
- `CustomAdapter.isMultiSelectMode` flag
- Bottom app bar switches from primary to utility menu
- Bulk actions: delete, complete, archive, star
- Undo support for certain operations

### 7.6 Search Filter DSL
`SearchFilter` parser allows complex queries:
```
tag:urgent priority:high "text search"
```

### 7.7 Swipe Gestures with Custom Drawing
- `ItemTouchHelper.SimpleCallback` with custom onChildDraw
- Left swipe (red): delete with trash icon
- Right swipe (green): complete with checkmark icon

### 7.8 Debounced Sync
```kotlin
private var pendingJob: Job? = null
private fun scheduleSync() {
    pendingJob?.cancel()
    pendingJob = scope.launch {
        delay(800)  // Debounce
        try { sync.syncOnce() } catch (_: Exception) {}
    }
}
```

### 7.9 Auto-Refresh with Handler
```kotlin
private val autoRefreshRunnable = object : Runnable {
    override fun run() {
        viewModel.loadData(currentType, silent = true)
        handler.postDelayed(this, 30000)  // Every 30s
    }
}
```

### 7.10 Celebration Animations
- Lottie for complex animations
- Konfetti for particle effects on task completion
- Configurable via `GlobalUtils.fireworksOnFinish`

### 7.11 Global Utils Singleton
`GlobalUtils.kt` provides:
- Preference access (SharedPreferences)
- Device detection (foldable, tablet)
- DateTime utilities
- Vibration feedback
- Multi-language support
- Color utilities

### 7.12 KeyStore for Credential Storage
`KeystorePreferencesManager`:
- Encrypts API keys and WebDAV passwords
- Uses Android Keystore system
- Protects sensitive configuration data

### 7.13 Activity Result API
- `registerForActivityResult()` for modern permission/activity handling
- Replaces deprecated `startActivityForResult()`

### 7.14 ViewCompositionStrategy in Embedded Compose
```kotlin
composeOverlay.setViewCompositionStrategy(
    ViewCompositionStrategy.DisposeOnDetachedFromWindow
)
```
Ensures proper lifecycle management of Compose in traditional views.

### 7.15 AlarmManager with Context-Based Wake-up
- Uses `AlarmClockInfo` for user-visible alarms
- Integrates with system lock screen
- Sets foreground service intent for notification

---

## 8. Key Dependencies

### AndroidX
- `androidx.lifecycle:lifecycle-runtime-ktx` - ViewModel, LiveData
- `androidx.navigation:navigation-compose` - Compose navigation
- `androidx.work:work-runtime-ktx` - WorkManager background tasks
- `androidx.window:window` - Foldable/tablet support
- `androidx.core:core` - Modern utilities

### UI
- `androidx.compose.material3:material3` - Material Design 3
- `androidx.appcompat:appcompat` - Traditional views
- `com.google.android.material:material` - Material components
- `nl.dionsegijn.konfetti:konfetti-xml` - Particle effects

### Network
- `com.squareup.okhttp3:okhttp` - HTTP client
- `com.google.code.gson:gson` - JSON serialization

### Other
- `io.noties.markwon:core` - Markdown rendering
- `com.airbnb.android:lottie` - Animations
- `io.github.ehsannarmani:compose-charts` - Charts
- `com.github.jeziellago:compose-markdown` - Markdown in Compose

---

## 9. Summary

**Architecture Style**: MVVM with Repository Pattern  
**UI Framework**: Hybrid (Traditional Views + Compose)  
**Data Storage**: SQLite with HLC versioning  
**Cloud Sync**: WebDAV snapshot-based  
**Background Work**: AlarmManager + WorkManager  
**State Management**: LiveData + StateFlow  
**Concurrency**: Coroutines with Dispatchers.IO

The codebase demonstrates modern Android development practices with a focus on:
- Clean separation of concerns
- Reactive data flow
- Comprehensive notification system
- Cross-device synchronization
- Accessibility (Material Design 3, edge-to-edge)
- Extensible settings architecture

