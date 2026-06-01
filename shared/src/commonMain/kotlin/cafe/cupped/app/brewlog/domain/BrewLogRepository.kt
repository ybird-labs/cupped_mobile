package cafe.cupped.app.brewlog.domain

// Local-first brew-log data boundary.
// Generated OpenAPI DTOs should not appear in this interface.
interface BrewLogRepository {
    suspend fun getOptions(): Result<BrewLogOptions>

    suspend fun createBrewLog(draft: BrewLogDraft): Result<LocalBrewLog>

    suspend fun getBrewLogs(): Result<List<LocalBrewLog>>
}
