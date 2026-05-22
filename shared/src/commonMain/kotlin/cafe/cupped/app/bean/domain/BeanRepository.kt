package cafe.cupped.app.bean.domain

// TODO: Define the domain boundary for bean/coffee lookup and future creation.
// Generated OpenAPI DTOs should not appear in this interface.
interface BeanRepository {
    // TODO: Search existing beans for the log-brew picker.
    suspend fun searchBeans(query: String): Result<List<BeanSearchResult>>

    // TODO: Fetch a single bean if/when needed by details screens.
    suspend fun getBean(id: String): Result<Bean>

    // TODO: Future endpoint: create a new bean/coffee if the API supports it.
    suspend fun createBean(draft: BeanDraft): Result<Bean>
}
