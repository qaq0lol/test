package com.pilltrack.nativeapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class PillTrackWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PillTrackWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.pilltrack_widget)

            // Load data
            val logs = LocalStorage.loadLogs(context)
            val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val todayStr = dayFormat.format(Date())
            val todayLogs = logs.filter { dayFormat.format(Date(it.time)) == todayStr }

            // Update total count
            views.setTextViewText(R.id.widget_streak_text, "累计 ${logs.size} 次")

            // Update today count
            views.setTextViewText(R.id.widget_today_count, "已记录 ${todayLogs.size} 次")

            // Update latest medication
            if (todayLogs.isNotEmpty()) {
                val latest = todayLogs.last()
                views.setTextViewText(R.id.widget_last_med_text, "⏱️ 最近: ${latest.name} ${latest.dose} · ${timeFormat.format(Date(latest.time))}")
            } else if (logs.isNotEmpty()) {
                val latest = logs.last()
                val dateStr = dayFormat.format(Date(latest.time)).substring(5)
                views.setTextViewText(R.id.widget_last_med_text, "⏱️ 上次: ${latest.name} ${latest.dose} · $dateStr")
            } else {
                views.setTextViewText(R.id.widget_last_med_text, "💡 点击桌面微件快速打开打卡")
            }

            // Click pending intent to launch MainActivity
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingIntentFlags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
