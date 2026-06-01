package cafe.cupped.app.brewlog.domain

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import cafe.cupped.app.brewmethod.domain.BrewMethod
import cafe.cupped.app.flavornote.domain.FlavorNote

/** Local SQLDelight-backed brew-log projection, distinct from server-confirmed [BrewLog]. */
data class LocalBrewLog(
    val id: String,
    val profileId: String,
    val bean: LocalBeanRef,
    val brewMethod: BrewMethod? = null,
    val flavorNotes: List<FlavorNote> = emptyList(),
    val rating: BrewRating? = null,
    val notes: String? = null,
    val location: BrewLogLocation? = null,
    val loggedAtIso: String? = null,
    val syncStatus: LocalSyncStatus,
    val localRevision: Long,
    val createdAtMillis: Long,
    val localUpdatedAtMillis: Long,
    val deletedAtMillis: Long? = null,
    val lastSyncError: String? = null,
)

sealed interface LocalBeanRef {
    data class Existing(val bean: Bean) : LocalBeanRef
    data class Optimistic(
        val id: String,
        val draft: BeanDraft,
        val syncState: LocalDependencySyncState,
    ) : LocalBeanRef
}

enum class LocalDependencySyncState {
    Pending,
    Blocked,
    Unknown,
}

enum class LocalSyncStatus(val storageValue: String) {
    Synced("synced"),
    PendingCreate("pending_create"),
    PendingUpdate("pending_update"),
    PendingDelete("pending_delete"),
    BlockedError("blocked_error"),
    AuthBlocked("auth_blocked"),
    NoOp("no_op"),
    Unknown("unknown");

    companion object {
        fun fromStorage(value: String): LocalSyncStatus = entries.firstOrNull { it.storageValue == value } ?: Unknown
    }
}

sealed class LocalBrewLogException(message: String) : IllegalStateException(message) {
    object ProfileRequired : LocalBrewLogException("Current profile is required for local brew-log writes")
    object ClientIdRequired : LocalBrewLogException("Stable client id is required for local brew-log writes")
    object OptimisticBeanMissing : LocalBrewLogException("Optimistic bean is not available for local brew-log write")
    object PersistenceFailed : LocalBrewLogException("Local brew-log write failed")
}
