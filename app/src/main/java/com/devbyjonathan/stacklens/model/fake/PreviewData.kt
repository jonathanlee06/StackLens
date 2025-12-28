package com.devbyjonathan.stacklens.model.fake

import com.devbyjonathan.stacklens.model.CrashFilter
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.screen.list.CrashLogUiState

object PreviewData {
    private val currentTime = System.currentTimeMillis()

    val sampleCrashLogs = listOf(
        CrashLog(
            id = 1,
            tag = CrashType.DATA_APP_CRASH,
            packageName = "com.example.myapp",
            appName = "My Application",
            timestamp = currentTime - 1000 * 60 * 5, // 5 minutes ago
            content = """
                java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.TextView.setText(java.lang.CharSequence)' on a null object reference
                    at com.example.myapp.MainActivity.onCreate(MainActivity.kt:42)
                    at android.app.Activity.performCreate(Activity.java:8051)
                    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1329)
            """.trimIndent(),
            processName = "com.example.myapp",
            pid = 12345
        ),
        CrashLog(
            id = 2,
            tag = CrashType.DATA_APP_ANR,
            packageName = "com.example.shopping",
            appName = "Shopping App",
            timestamp = currentTime - 1000 * 60 * 15, // 15 minutes ago
            content = """
                ANR in com.example.shopping
                PID: 23456
                Reason: Input dispatching timed out (Waiting to send non-key event because the touched window has not finished processing)
                Parent: com.example.shopping/.MainActivity
            """.trimIndent(),
            processName = "com.example.shopping",
            pid = 23456
        ),
        CrashLog(
            id = 3,
            tag = CrashType.SYSTEM_TOMBSTONE,
            packageName = "com.example.game",
            appName = "Awesome Game",
            timestamp = currentTime - 1000 * 60 * 30, // 30 minutes ago
            content = """
                *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
                Build fingerprint: 'google/pixel6/pixel6:14/UP1A.231005.007/10754064:user/release-keys'
                Abort message: 'FORTIFY: pthread_mutex_lock called on a destroyed mutex'
                    #00 pc 0x0000000000089abc  /apex/com.android.runtime/lib64/bionic/libc.so (abort+164)
                    #01 pc 0x0000000000042def  /apex/com.android.runtime/lib64/bionic/libc.so
            """.trimIndent(),
            processName = "com.example.game",
            pid = 34567
        ),
        CrashLog(
            id = 4,
            tag = CrashType.DATA_APP_CRASH,
            packageName = "com.social.messenger",
            appName = "Messenger",
            timestamp = currentTime - 1000 * 60 * 60, // 1 hour ago
            content = """
                java.lang.IllegalStateException: Fragment MessageFragment not attached to a context.
                    at androidx.fragment.app.Fragment.requireContext(Fragment.java:900)
                    at com.social.messenger.ui.MessageFragment.loadMessages(MessageFragment.kt:156)
                    at com.social.messenger.ui.MessageFragment.onViewCreated(MessageFragment.kt:78)
            """.trimIndent(),
            processName = "com.social.messenger",
            pid = 45678
        ),
        CrashLog(
            id = 5,
            tag = CrashType.DATA_APP_ANR,
            packageName = "com.weather.forecast",
            appName = "Weather Forecast",
            timestamp = currentTime - 1000 * 60 * 60 * 2, // 2 hours ago
            content = """
                ANR in com.weather.forecast
                PID: 56789
                Reason: executing service com.weather.forecast/.sync.WeatherSyncService
                Parent: com.weather.forecast/.MainActivity
            """.trimIndent(),
            processName = "com.weather.forecast",
            pid = 56789
        )
    )

    val sampleStats: Map<CrashType, Int> = mapOf(
        CrashType.DATA_APP_CRASH to 12,
        CrashType.SYSTEM_APP_CRASH to 3,
        CrashType.DATA_APP_ANR to 5,
        CrashType.SYSTEM_APP_ANR to 1,
        CrashType.SYSTEM_TOMBSTONE to 2
    )

    val sampleUiState = CrashLogUiState(
        isLoading = false,
        hasPermissions = true,
        hasReadLogsPermission = true,
        hasUsageStatsPermission = true,
        hasDropBoxDataPermission = true,
        crashLogs = sampleCrashLogs,
        stats = sampleStats,
        filter = CrashFilter(),
        error = null
    )
}