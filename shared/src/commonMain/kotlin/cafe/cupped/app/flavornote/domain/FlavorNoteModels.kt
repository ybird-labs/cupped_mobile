package cafe.cupped.app.flavornote.domain

// A flavor note the user can attach to a brew log.
data class FlavorNote(
    val id: String,
    val name: String,
    val slug: String,
    val category: String? = null
)
