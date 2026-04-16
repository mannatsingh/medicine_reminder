package com.mannat.medicine_reminder.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineNameProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var allNames: List<String> = emptyList()

    private suspend fun ensureLoaded() {
        if (allNames.isNotEmpty()) return
        withContext(Dispatchers.IO) {
            val json = context.assets.open("medicines.json")
                .bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val names = mutableListOf<String>()
            for (i in 0 until array.length()) {
                names.add(array.getString(i))
            }
            allNames = names
        }
    }

    suspend fun search(query: String, limit: Int = 20): List<String> {
        if (query.length < 2) return emptyList()
        ensureLoaded()
        val lower = query.lowercase()
        return allNames
            .filter { it.lowercase().contains(lower) }
            .sortedBy {
                // Prefer names that start with the query
                if (it.lowercase().startsWith(lower)) 0 else 1
            }
            .take(limit)
    }
}
