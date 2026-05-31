package cafe.cupped.app.brewlog.domain

/**
 * Supplies the active Brewer profile id used to scope local brew-log rows and
 * future sync outbox work.
 *
 * Auth account ids are intentionally not used here: Brewer brew logs are
 * profile-scoped. Implementations should return null when no profile context is
 * available so local writes can fail closed instead of writing under a
 * placeholder or auth user id.
 */
fun interface CurrentProfileProvider {
    fun currentProfileId(): String?
}

/** Supplies the stable per-install client id written to sync outbox rows. */
fun interface SyncClientIdProvider {
    fun clientId(): String
}
