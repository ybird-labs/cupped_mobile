package cafe.cupped.app.brewlog.domain

// TODO: Define the domain boundary for brew log data.
// Generated OpenAPI DTOs should not appear in this interface.
interface BrewLogRepository {
    // TODO: Load brew methods, flavor notes, and rating range.
    suspend fun getOptions(): Result<BrewLogOptions>

    // TODO: Submit a draft and return the saved brew log.
    suspend fun createBrewLog(draft: BrewLogDraft): Result<BrewLog>

    // TODO: Load existing brew logs for journal/feed use cases.
    suspend fun getBrewLogs(): Result<List<BrewLog>>
}
