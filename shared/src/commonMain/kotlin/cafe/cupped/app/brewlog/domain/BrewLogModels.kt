package cafe.cupped.app.brewlog.domain

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import cafe.cupped.app.brewmethod.domain.BrewMethod
import cafe.cupped.app.flavornote.domain.FlavorNote
import cafe.cupped.app.recipe.domain.Recipe
import cafe.cupped.app.recipe.domain.RecipeDraft

// Form state for a brew log before it is submitted.
data class BrewLogDraft(
    val bean: SelectedBean? = null,
    val brewMethod: BrewMethod? = null,
    val flavorNotes: List<FlavorNote> = emptyList(),
    val rating: BrewRating? = null,
    val notes: String = "",
    val location: BrewLogLocation? = null,
    val mediaIds: List<String> = emptyList(),
    val loggedAtIso: String? = null,
    val createPost: Boolean = false,
    val recipe: SelectedRecipe? = null
)

// The bean choice in the log-brew form.
sealed interface SelectedBean {
    data class Existing(val bean: Bean) : SelectedBean
    data class NewDraft(val draft: BeanDraft) : SelectedBean
    data class Optimistic(val id: String) : SelectedBean
}

// The recipe choice in the log-brew form.
sealed interface SelectedRecipe {
    data class Existing(val recipe: Recipe) : SelectedRecipe
    data class NewDraft(val draft: RecipeDraft) : SelectedRecipe
}

// Rating uses the API's 1-100 scale as the common/domain scale.
data class BrewRating(
    val valueOutOf100: Int
) {
    init {
        require(valueOutOf100 in 1..100) { "Brew rating must be between 1 and 100" }
    }
}

// Platform-neutral place or location details for a brew log.
data class BrewLogLocation(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeId: String? = null
)

// A saved brew log returned by the backend.
data class BrewLog(
    val id: String,
    val bean: Bean,
    val brewMethod: BrewMethod? = null,
    val flavorNotes: List<FlavorNote> = emptyList(),
    val rating: BrewRating? = null,
    val notes: String? = null,
    val locationName: String? = null,
    val media: List<BrewLogMedia> = emptyList(),
    val loggedAtIso: String? = null,
    val recipe: Recipe? = null
)

// Media attached to a saved brew log.
data class BrewLogMedia(
    val id: String,
    val mediaType: String,
    val url: String? = null,
    val altText: String? = null,
    val position: Int? = null
)

// Options needed to render the log-brew form.
data class BrewLogOptions(
    val brewMethods: List<BrewMethod> = emptyList(),
    val flavorNotes: List<FlavorNote> = emptyList(),
    val ratingRange: RatingRange = RatingRange(min = 1, max = 100)
)

// Inclusive rating bounds for the current API/domain scale.
data class RatingRange(
    val min: Int,
    val max: Int
)
