package app.coreme.messenger.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coreme_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    val themeFlow: Flow<String> = store.data.map { it[THEME_KEY] ?: "dark" }

    suspend fun setTheme(theme: String) {
        store.edit { it[THEME_KEY] = theme }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
    }
}
