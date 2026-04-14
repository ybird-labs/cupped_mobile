package cafe.cupped.app.locale

enum class SupportedLanguage(
    val bcp47Tag: String,
    val fallbackTags: List<String> = emptyList(),
) {
    EN("en"),
    ES("es"),
    ES_419("es-419", fallbackTags = listOf("es")),
}

data class LanguagePreference(
    val selected: SupportedLanguage?,
)

interface LanguagePreferenceRepository {
    suspend fun getPreference(): LanguagePreference
    suspend fun setPreference(preference: LanguagePreference)
}
