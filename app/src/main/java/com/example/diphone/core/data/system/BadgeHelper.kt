package com.example.diphone.core.data.system

import android.content.Context
import android.content.SharedPreferences
import me.leolin.shortcutbadger.ShortcutBadger

object BadgeHelper {
    private const val PREF_NAME = "badge_prefs"
    private const val BADGE_COUNT_KEY = "badge_count"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setBadgeCount(context: Context, count: Int) {
        try {
            getPreferences(context).edit().putInt(BADGE_COUNT_KEY, count).apply()
            if (count > 0) {
                ShortcutBadger.applyCount(context, count)
            } else {
                ShortcutBadger.removeCount(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeBadge(context: Context) {
        try {
            getPreferences(context).edit().putInt(BADGE_COUNT_KEY, 0).apply()
            ShortcutBadger.removeCount(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun incrementBadge(context: Context) {
        try {
            val prefs = getPreferences(context)
            val currentCount = prefs.getInt(BADGE_COUNT_KEY, 0)
            val newCount = currentCount + 1
            prefs.edit().putInt(BADGE_COUNT_KEY, newCount).apply()
            ShortcutBadger.applyCount(context, newCount)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
