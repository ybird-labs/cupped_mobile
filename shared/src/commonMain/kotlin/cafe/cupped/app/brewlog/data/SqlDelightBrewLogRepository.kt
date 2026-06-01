package cafe.cupped.app.brewlog.data

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import cafe.cupped.app.brewlog.domain.BeanIdProvider
import cafe.cupped.app.brewlog.domain.BrewLogDraft
import cafe.cupped.app.brewlog.domain.BrewLogIdProvider
import cafe.cupped.app.brewlog.domain.BrewLogLocation
import cafe.cupped.app.brewlog.domain.BrewLogOptions
import cafe.cupped.app.brewlog.domain.BrewLogRepository
import cafe.cupped.app.brewlog.domain.BrewRating
import cafe.cupped.app.brewlog.domain.CurrentProfileProvider
import cafe.cupped.app.brewlog.domain.EpochMillisProvider
import cafe.cupped.app.brewlog.domain.LocalBeanRef
import cafe.cupped.app.brewlog.domain.LocalBrewLog
import cafe.cupped.app.brewlog.domain.LocalBrewLogException
import cafe.cupped.app.brewlog.domain.LocalDependencySyncState
import cafe.cupped.app.brewlog.domain.LocalSyncStatus
import cafe.cupped.app.brewlog.domain.OutboxIdProvider
import cafe.cupped.app.brewlog.domain.SelectedBean
import cafe.cupped.app.brewlog.domain.SyncClientIdProvider
import cafe.cupped.app.db.Bean_cache
import cafe.cupped.app.db.Brew_logs
import cafe.cupped.app.db.CuppedDatabase
import cafe.cupped.app.db.Sync_outbox
import cafe.cupped.app.db.TransactionDispatcher
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SqlDelightBrewLogRepository(
    private val database: CuppedDatabase,
    private val transactionDispatcher: TransactionDispatcher,
    private val currentProfileProvider: CurrentProfileProvider,
    private val syncClientIdProvider: SyncClientIdProvider,
    private val brewLogIdProvider: BrewLogIdProvider,
    private val beanIdProvider: BeanIdProvider,
    private val outboxIdProvider: OutboxIdProvider,
    private val epochMillisProvider: EpochMillisProvider,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
) : BrewLogRepository {

    override suspend fun getOptions(): Result<BrewLogOptions> = Result.success(BrewLogOptions())

    override suspend fun createBrewLog(draft: BrewLogDraft): Result<LocalBrewLog> = withContext(transactionDispatcher.dispatcher) {
        val profileId = currentProfileProvider.currentProfileId().sanitizeOrNull()
            ?: return@withContext Result.failure(LocalBrewLogException.ProfileRequired)
        val clientId = syncClientIdProvider.clientId().sanitizeOrNull()
            ?: return@withContext Result.failure(LocalBrewLogException.ClientIdRequired)

        try {
            when (val selectedBean = draft.bean) {
                is SelectedBean.Existing -> createWithExistingBean(profileId, clientId, draft, selectedBean.bean)
                is SelectedBean.NewDraft -> createWithNewBeanDraft(profileId, clientId, draft, selectedBean.draft)
                is SelectedBean.Optimistic -> createWithOptimisticBean(profileId, clientId, draft, selectedBean.id)
                null -> Result.failure(LocalBrewLogException.PersistenceFailed)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Result.failure(LocalBrewLogException.PersistenceFailed)
        }
    }

    override suspend fun getBrewLogs(): Result<List<LocalBrewLog>> = withContext(transactionDispatcher.dispatcher) {
        val profileId = currentProfileProvider.currentProfileId().sanitizeOrNull()
            ?: return@withContext Result.failure(LocalBrewLogException.ProfileRequired)
        try {
            Result.success(
                database.brewLogQueries.selectLocalJournalByProfile(profileId)
                    .executeAsList()
                    .mapNotNull { row -> row.toLocalBrewLogOrNull(profileId) }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            Result.failure(LocalBrewLogException.PersistenceFailed)
        }
    }

    private fun createWithExistingBean(
        profileId: String,
        clientId: String,
        draft: BrewLogDraft,
        bean: Bean,
    ): Result<LocalBrewLog> {
        val brewLogId = brewLogIdProvider.nextBrewLogId()
        val outboxId = outboxIdProvider.nextOutboxId(ENTITY_BREW_LOG, brewLogId, OP_CREATE)
        val now = epochMillisProvider.nowMillis()

        var localBrewLog: LocalBrewLog? = null
        database.transaction {
            database.referenceCacheQueries.upsertExistingBeanCache(
                id = bean.id,
                name = bean.name,
                slug = bean.slug,
                country = bean.country,
                region = bean.region,
                process = bean.process,
                roast_level = bean.roastLevel?.toLong(),
            )
            insertBrewLog(
                id = brewLogId,
                profileId = profileId,
                beanId = bean.id,
                beanClientId = null,
                beanDraftJson = null,
                draft = draft,
                now = now,
            )
            insertOutbox(
                id = outboxId,
                profileId = profileId,
                clientId = clientId,
                entityType = ENTITY_BREW_LOG,
                entityId = brewLogId,
                operation = OP_CREATE,
                now = now,
                dependsOnEntityType = null,
                dependsOnEntityId = null,
            )
            localBrewLog = database.brewLogQueries.selectLocalBrewLogById(profileId, brewLogId)
                .executeAsOne()
                .toLocalBrewLogOrNull(profileId)
        }
        return localBrewLog?.let { Result.success(it) } ?: Result.failure(LocalBrewLogException.PersistenceFailed)
    }

    private fun createWithNewBeanDraft(
        profileId: String,
        clientId: String,
        draft: BrewLogDraft,
        beanDraft: BeanDraft,
    ): Result<LocalBrewLog> {
        val beanId = beanIdProvider.nextBeanId()
        val brewLogId = brewLogIdProvider.nextBrewLogId()
        val beanOutboxId = outboxIdProvider.nextOutboxId(ENTITY_BEAN, beanId, OP_CREATE)
        val brewLogOutboxId = outboxIdProvider.nextOutboxId(ENTITY_BREW_LOG, brewLogId, OP_CREATE)
        val now = epochMillisProvider.nowMillis()
        val payloadJson = json.encodeOptimisticBeanPayload(
            LocalOptimisticBeanPayload.fromDraft(beanId, beanDraft, now)
        )

        var localBrewLog: LocalBrewLog? = null
        database.transaction {
            database.referenceCacheQueries.insertOptimisticBean(
                id = beanId,
                profile_id = profileId,
                name = beanDraft.name,
                country = beanDraft.country,
                region = beanDraft.region,
                process = beanDraft.process,
                roast_level = beanDraft.roastLevel?.toLong(),
                payload_json = payloadJson,
            )
            insertOutbox(beanOutboxId, profileId, clientId, ENTITY_BEAN, beanId, OP_CREATE, now, null, null)
            insertBrewLog(
                id = brewLogId,
                profileId = profileId,
                beanId = beanId,
                beanClientId = beanId,
                beanDraftJson = null,
                draft = draft,
                now = now,
            )
            insertOutbox(brewLogOutboxId, profileId, clientId, ENTITY_BREW_LOG, brewLogId, OP_CREATE, now, ENTITY_BEAN, beanId)
            localBrewLog = database.brewLogQueries.selectLocalBrewLogById(profileId, brewLogId)
                .executeAsOne()
                .toLocalBrewLogOrNull(profileId)
        }
        return localBrewLog?.let { Result.success(it) } ?: Result.failure(LocalBrewLogException.PersistenceFailed)
    }

    private fun createWithOptimisticBean(
        profileId: String,
        clientId: String,
        draft: BrewLogDraft,
        beanId: String,
    ): Result<LocalBrewLog> {
        val brewLogId = brewLogIdProvider.nextBrewLogId()
        val brewLogOutboxId = outboxIdProvider.nextOutboxId(ENTITY_BREW_LOG, brewLogId, OP_CREATE)
        val now = epochMillisProvider.nowMillis()

        var optimisticBeanMissing = false
        var localBrewLog: LocalBrewLog? = null
        database.transaction {
            val bean = database.referenceCacheQueries.selectOptimisticBeanForProfile(beanId, profileId).executeAsOneOrNull()
            val activeBeanCreate = database.syncOutboxQueries.selectActiveBeanCreateOutbox(profileId, beanId).executeAsOneOrNull()
            if (bean?.payload_json == null || activeBeanCreate == null) {
                optimisticBeanMissing = true
                return@transaction
            }

            insertBrewLog(
                id = brewLogId,
                profileId = profileId,
                beanId = beanId,
                beanClientId = beanId,
                beanDraftJson = null,
                draft = draft,
                now = now,
            )
            insertOutbox(brewLogOutboxId, profileId, clientId, ENTITY_BREW_LOG, brewLogId, OP_CREATE, now, ENTITY_BEAN, beanId)
            localBrewLog = database.brewLogQueries.selectLocalBrewLogById(profileId, brewLogId)
                .executeAsOne()
                .toLocalBrewLogOrNull(profileId)
        }
        if (optimisticBeanMissing) return Result.failure(LocalBrewLogException.OptimisticBeanMissing)
        return localBrewLog?.let { Result.success(it) } ?: Result.failure(LocalBrewLogException.PersistenceFailed)
    }

    private fun insertBrewLog(
        id: String,
        profileId: String,
        beanId: String,
        beanClientId: String?,
        beanDraftJson: String?,
        draft: BrewLogDraft,
        now: Long,
    ) {
        database.brewLogQueries.insertPendingCreate(
            id = id,
            profile_id = profileId,
            bean_id = beanId,
            bean_client_id = beanClientId,
            bean_draft_json = beanDraftJson,
            recipe_id = (draft.recipe as? cafe.cupped.app.brewlog.domain.SelectedRecipe.Existing)?.recipe?.id,
            recipe_client_id = null,
            recipe_draft_json = null,
            brew_method_id = draft.brewMethod?.id,
            flavor_note_ids = draft.flavorNotes.takeIf { it.isNotEmpty() }?.map { it.id }?.let { json.encodeToString(it) },
            rating = draft.rating?.valueOutOf100?.toLong(),
            notes = draft.notes.takeIf { it.isNotBlank() },
            latitude = draft.location?.latitude,
            longitude = draft.location?.longitude,
            location_name = draft.location?.name,
            place_id = draft.location?.placeId,
            logged_at_iso = draft.loggedAtIso,
            created_at = now,
            local_updated_at = now,
        )
    }

    private fun insertOutbox(
        id: String,
        profileId: String,
        clientId: String,
        entityType: String,
        entityId: String,
        operation: String,
        now: Long,
        dependsOnEntityType: String?,
        dependsOnEntityId: String?,
    ) {
        database.syncOutboxQueries.insertOutbox(
            id = id,
            profile_id = profileId,
            client_id = clientId,
            entity_type = entityType,
            entity_id = entityId,
            operation = operation,
            created_at = now,
            depends_on_entity_type = dependsOnEntityType,
            depends_on_entity_id = dependsOnEntityId,
        )
    }

    private fun Brew_logs.toLocalBrewLogOrNull(profileId: String): LocalBrewLog? {
        val beanId = bean_id ?: return null
        val beanRef = if (bean_client_id != null) {
            val bean = database.referenceCacheQueries.selectOptimisticBeanForProfile(beanId, profileId).executeAsOneOrNull()
                ?: return null
            val payloadJson = bean.payload_json ?: return null
            val draft = json.decodeOptimisticBeanPayload(payloadJson).toDraft()
            val outbox = database.syncOutboxQueries.selectActiveOutboxForEntity(profileId, ENTITY_BEAN, beanId).executeAsOneOrNull()
            LocalBeanRef.Optimistic(
                id = beanId,
                draft = draft,
                syncState = outbox.toDependencySyncState(),
            )
        } else {
            val bean = database.referenceCacheQueries.selectBeanForProfile(beanId, profileId).executeAsOneOrNull()
                ?: return null
            LocalBeanRef.Existing(bean.toDomainBean())
        }

        return LocalBrewLog(
            id = id,
            profileId = profile_id,
            bean = beanRef,
            rating = rating?.toInt()?.let(::BrewRating),
            notes = notes,
            location = BrewLogLocation(
                name = location_name,
                latitude = latitude,
                longitude = longitude,
                placeId = place_id,
            ).takeIf { it.name != null || it.latitude != null || it.longitude != null || it.placeId != null },
            loggedAtIso = logged_at_iso,
            syncStatus = LocalSyncStatus.fromStorage(sync_status),
            localRevision = local_revision,
            createdAtMillis = created_at,
            localUpdatedAtMillis = local_updated_at,
            deletedAtMillis = deleted_at,
            lastSyncError = last_sync_error,
        )
    }

    private fun Bean_cache.toDomainBean(): Bean = Bean(
        id = id,
        name = name,
        slug = slug,
        country = country,
        region = region,
        process = process,
        roastLevel = roast_level?.toInt(),
    )

    private fun Sync_outbox?.toDependencySyncState(): LocalDependencySyncState = when (this?.state) {
        "pending", "in_flight" -> LocalDependencySyncState.Pending
        "blocked_error", "auth_blocked" -> LocalDependencySyncState.Blocked
        else -> LocalDependencySyncState.Unknown
    }

    private fun String?.sanitizeOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val ENTITY_BREW_LOG = "brew_log"
        const val ENTITY_BEAN = "bean"
        const val OP_CREATE = "create"
    }
}
