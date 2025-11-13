package com.aritxonly.deadliner.widgets

import android.content.Context
import android.content.Intent
import android.content.ComponentName

/**
 * Helper class to send widget update broadcasts
 */
object WidgetUpdateHelper {
    const val ACTION_WIDGET_UPDATE = "com.aritxonly.deadliner.ACTION_WIDGET_UPDATE"
    const val ACTION_WIDGET_REFRESH = "com.aritxonly.deadliner.ACTION_WIDGET_REFRESH"

    /**
     * Send broadcast to update all widgets
     */
    fun sendUpdateBroadcast(context: Context) {
        val intent = Intent(ACTION_WIDGET_UPDATE).apply {
            component = ComponentName(
                "com.aritxonly.deadliner",
                "com.aritxonly.deadliner.widgets.MultiDeadlineWidget"
            )
            `package` = "com.aritxonly.deadliner"
        }
        context.sendBroadcast(intent)
    }

    /**
     * Send broadcast to refresh widgets (used by refresh button)
     */
    fun sendRefreshBroadcast(context: Context) {
        val intent = Intent(ACTION_WIDGET_REFRESH).apply {
            component = ComponentName(
                "com.aritxonly.deadliner",
                "com.aritxonly.deadliner.widgets.MultiDeadlineWidget"
            )
            `package` = "com.aritxonly.deadliner"
        }
        context.sendBroadcast(intent)
    }
}
