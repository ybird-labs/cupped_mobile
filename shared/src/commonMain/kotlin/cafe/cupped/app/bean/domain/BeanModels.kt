package cafe.cupped.app.bean.domain

data class Bean(
    val id: String,
    val name: String,
    val slug: String? = null,
    val country: String? = null,
    val region: String? = null,
    val process: String? = null,
    val roastLevel: Int? = null
)

// Editable bean details captured before the user saves or selects a bean.
data class BeanDraft(
    val name: String,
    val country: String? = null,
    val region: String? = null,
    val farm: String? = null,
    val producer: String? = null,
    val roaster: String? = null,
    val process: String? = null,
    val roastLevel: Int? = null
)

// A bean result formatted for search and picker screens.
data class BeanSearchResult(
    val bean: Bean,
    val subtitle: String? = null,
    val imageUrl: String? = null
)
