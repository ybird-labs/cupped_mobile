package cafe.cupped.app.recipe.domain

// A saved recipe that can be reused when logging a brew.
data class Recipe(
    val id: String,
    val name: String,
    val grindSize: Int? = null,
    val waterTempFahrenheit: Int? = null,
    val ratio: Double? = null
)

// Editable recipe values captured before the user saves or selects a recipe.
data class RecipeDraft(
    val name: String? = null,
    val grindSize: Int? = null,
    val waterTempFahrenheit: Int? = null,
    val ratio: Double? = null
)
