package pk.codehub.connectify.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension function to create DataStore instance in context
private val Context.dataStore by preferencesDataStore("app_preferences")

object DataStoreManager {

    // Function to save a value to DataStore
    suspend fun saveValue(context: Context, key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[dataStoreKey] = value
        }
    }

    // Function to retrieve a value from DataStore
    fun getValue(context: Context, key: String, defaultValue: String): Flow<String> {
        val dataStoreKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }
    }

}
