package com.devbyjonathan.stacklens.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

object PermissionChecker {

    fun hasReadLogsPermission(context: Context): Boolean {
        return context.checkPermission(
            android.Manifest.permission.READ_LOGS,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadDropBoxDataPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            true
        } else {
            context.checkPermission(
                android.Manifest.permission.READ_DROPBOX_DATA,
                Process.myPid(),
                Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasReadLogsPermission(context) && hasUsageStatsPermission(context) && hasReadDropBoxDataPermission(context)
    }
}
