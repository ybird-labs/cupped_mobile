package cafe.cupped.app.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cafe.cupped.app.locale.LanguagePreference
import cafe.cupped.app.locale.SupportedLanguage

class AndroidAppLanguageManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPreference(): LanguagePreference {
        val storedTag = preferences.getString(KEY_SELECTED_LANGUAGE, null)
        val selectedLanguage = SupportedLanguage.entries.firstOrNull { it.bcp47Tag == storedTag }
        return LanguagePreference(selected = selectedLanguage)
    }

    fun updatePreference(selectedLanguage: SupportedLanguage?) {
        if (selectedLanguage == null) {
            preferences.edit().remove(KEY_SELECTED_LANGUAGE).apply()
        } else {
            preferences.edit().putString(KEY_SELECTED_LANGUAGE, selectedLanguage.bcp47Tag).apply()
        }
        applyPreference(LanguagePreference(selectedLanguage))
    }

    fun applyStoredPreference() {
        applyPreference(getPreference())
    }

    private fun applyPreference(preference: LanguagePreference) {
        val locales = preference.selected?.let { LocaleListCompat.forLanguageTags(it.bcp47Tag) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private companion object {
        const val PREFS_NAME = "app_localization"
        const val KEY_SELECTED_LANGUAGE = "selected_language"
    }
}
