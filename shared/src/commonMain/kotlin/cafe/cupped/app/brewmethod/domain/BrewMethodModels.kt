package cafe.cupped.app.brewmethod.domain

// A brewing method the user can choose when logging a brew.
data class BrewMethod(
    val id: String,
    val name: String,
    val slug: String
)
