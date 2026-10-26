package com.nobadhabbits.cornytask.features.main

import android.content.Context

/** Remembers the order of the navigation buttons on this device. */
class NavOrderPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("nav_prefs", Context.MODE_PRIVATE)

    fun getOrder(): List<Destination> {
        val saved = prefs.getString(KEY_ORDER, null)
            ?.split(",")
            ?.mapNotNull { name -> Destination.entries.firstOrNull { it.name == name } }
            .orEmpty()
        // Append anything new (or missing) so an app update never hides a screen.
        return (saved + Destination.entries).distinct()
    }

    fun setOrder(order: List<Destination>) {
        prefs.edit().putString(KEY_ORDER, order.joinToString(",") { it.name }).apply()
    }

    companion object {
        private const val KEY_ORDER = "order"
    }
}
