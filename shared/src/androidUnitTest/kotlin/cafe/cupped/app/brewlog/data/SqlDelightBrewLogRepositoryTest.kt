package cafe.cupped.app.brewlog.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import cafe.cupped.app.brewlog.domain.BeanIdProvider
import cafe.cupped.app.brewlog.domain.BrewLogDraft
import cafe.cupped.app.brewlog.domain.BrewLogIdProvider
import cafe.cupped.app.brewlog.domain.BrewLogRepository
import cafe.cupped.app.brewlog.domain.BrewRating
import cafe.cupped.app.brewlog.domain.CurrentProfileProvider
import cafe.cupped.app.brewlog.domain.EpochMillisProvider
import cafe.cupped.app.brewlog.domain.LocalBeanRef
import cafe.cupped.app.brewlog.domain.LocalBrewLogException
import cafe.cupped.app.brewlog.domain.LocalDependencySyncState
import cafe.cupped.app.brewlog.domain.LocalSyncStatus
import cafe.cupped.app.brewlog.domain.OutboxIdProvider
import cafe.cupped.app.brewlog.domain.SelectedBean
import cafe.cupped.app.brewlog.domain.SyncClientIdProvider
import cafe.cupped.app.db.CuppedDatabase
import cafe.cupped.app.db.TransactionDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SqlDelightBrewLogRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: CuppedDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuppedDatabase.Schema.create(driver)
        database = CuppedDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun createWithExistingBeanPersistsLocalLogAndOutboxAtomically() = runTest {
        val repository = repository(
            brewLogIds = listOf("brew-log-1"),
            beanIds = listOf("unused-bean-id"),
            outboxIds = listOf("outbox-brew-log-1"),
        )
        val bean = existingBean()
        val draft = BrewLogDraft(
            bean = SelectedBean.Existing(bean),
            rating = BrewRating(87),
            notes = "Sweet citrus and caramel",
            loggedAtIso = "2026-06-01T12:00:00Z",
        )

        val result = repository.createBrewLog(draft)

        assertTrue(result.isSuccess, "existing bean creates should return Result.success")
        val localBrewLog = result.getOrThrow()
        assertEquals("brew-log-1", localBrewLog.id)
        assertEquals("profile-1", localBrewLog.profileId)
        assertEquals(LocalSyncStatus.PendingCreate, localBrewLog.syncStatus)
        assertEquals(1L, localBrewLog.localRevision)
        assertEquals(1_717_171_717_000L, localBrewLog.createdAtMillis)
        assertEquals(1_717_171_717_000L, localBrewLog.localUpdatedAtMillis)
        assertNull(localBrewLog.deletedAtMillis)
        assertNull(localBrewLog.lastSyncError)
        val beanRef = assertIs<LocalBeanRef.Existing>(localBrewLog.bean)
        assertEquals(bean, beanRef.bean)

        assertEquals(1L, count("brew_logs", "profile_id = 'profile-1'"))
        assertEquals("brew-log-1", stringValue("SELECT id FROM brew_logs WHERE profile_id = 'profile-1'"))
        assertEquals("bean-1", stringValue("SELECT bean_id FROM brew_logs WHERE id = 'brew-log-1'"))
        assertNull(stringValue("SELECT bean_client_id FROM brew_logs WHERE id = 'brew-log-1'"))
        assertEquals("pending_create", stringValue("SELECT sync_status FROM brew_logs WHERE id = 'brew-log-1'"))
        assertEquals(1L, longValue("SELECT local_revision FROM brew_logs WHERE id = 'brew-log-1'"))
        assertEquals(1_717_171_717_000L, longValue("SELECT created_at FROM brew_logs WHERE id = 'brew-log-1'"))
        assertEquals(1_717_171_717_000L, longValue("SELECT local_updated_at FROM brew_logs WHERE id = 'brew-log-1'"))

        assertEquals(1L, count("sync_outbox", "profile_id = 'profile-1' AND entity_type = 'brew_log' AND entity_id = 'brew-log-1' AND operation = 'create' AND state = 'pending' AND client_id = 'client-1'"))
        assertEquals("outbox-brew-log-1", stringValue("SELECT id FROM sync_outbox WHERE entity_type = 'brew_log' AND entity_id = 'brew-log-1'"))
        assertNull(stringValue("SELECT depends_on_entity_type FROM sync_outbox WHERE id = 'outbox-brew-log-1'"))
        assertNull(stringValue("SELECT depends_on_entity_id FROM sync_outbox WHERE id = 'outbox-brew-log-1'"))
        assertEquals(0L, count("sync_outbox", "entity_type = 'bean' AND operation = 'create'"))
    }

    @Test
    fun createWithNewBeanDraftPersistsBeanLogAndDependentOutboxAtomically() = runTest {
        val repository = repository(
            brewLogIds = listOf("brew-log-2"),
            beanIds = listOf("bean-opt-1"),
            outboxIds = listOf("outbox-bean-1", "outbox-brew-log-2"),
        )
        val draft = BrewLogDraft(
            bean = SelectedBean.NewDraft(newBeanDraft()),
            notes = "first optimistic log",
        )

        val result = repository.createBrewLog(draft)

        assertTrue(result.isSuccess)
        val beanRef = assertIs<LocalBeanRef.Optimistic>(result.getOrThrow().bean)
        assertEquals("bean-opt-1", beanRef.id)
        assertEquals("Farm A", beanRef.draft.farm)
        assertEquals(LocalDependencySyncState.Pending, beanRef.syncState)
        assertEquals(1L, count("bean_cache", "id = 'bean-opt-1' AND profile_id = 'profile-1'"))
        assertEquals("bean-opt-1", stringValue("SELECT bean_id FROM brew_logs WHERE id = 'brew-log-2'"))
        assertEquals("bean-opt-1", stringValue("SELECT bean_client_id FROM brew_logs WHERE id = 'brew-log-2'"))
        assertEquals(1L, count("sync_outbox", "id = 'outbox-bean-1' AND entity_type = 'bean' AND entity_id = 'bean-opt-1' AND operation = 'create'"))
        assertEquals("bean", stringValue("SELECT depends_on_entity_type FROM sync_outbox WHERE id = 'outbox-brew-log-2'"))
        assertEquals("bean-opt-1", stringValue("SELECT depends_on_entity_id FROM sync_outbox WHERE id = 'outbox-brew-log-2'"))
    }

    @Test
    fun createWithOptimisticBeanReusesActiveBeanCreateOutbox() = runTest {
        repository(
            brewLogIds = listOf("brew-log-1"),
            beanIds = listOf("bean-opt-1"),
            outboxIds = listOf("outbox-bean-1", "outbox-brew-log-1"),
        ).createBrewLog(BrewLogDraft(bean = SelectedBean.NewDraft(newBeanDraft())))
        val repository = repository(
            brewLogIds = listOf("brew-log-2"),
            beanIds = listOf("unused"),
            outboxIds = listOf("outbox-brew-log-2"),
        )

        val result = repository.createBrewLog(BrewLogDraft(bean = SelectedBean.Optimistic("bean-opt-1")))

        assertTrue(result.isSuccess)
        val beanRef = assertIs<LocalBeanRef.Optimistic>(result.getOrThrow().bean)
        assertEquals("bean-opt-1", beanRef.id)
        assertEquals(2L, count("brew_logs", "bean_client_id = 'bean-opt-1'"))
        assertEquals(1L, count("sync_outbox", "profile_id = 'profile-1' AND entity_type = 'bean' AND entity_id = 'bean-opt-1' AND operation = 'create' AND state IN ('pending', 'in_flight', 'blocked_error', 'auth_blocked')"))
        assertEquals("bean-opt-1", stringValue("SELECT depends_on_entity_id FROM sync_outbox WHERE id = 'outbox-brew-log-2'"))
    }

    @Test
    fun createWithOptimisticBeanRejectsActiveNonCreateBeanOutboxAndWritesNothing() = runTest {
        insertOptimisticBeanRow(id = "bean-opt-update", profileId = "profile-1")
        insertBeanOutbox(id = "outbox-bean-update", profileId = "profile-1", beanId = "bean-opt-update", operation = "update")

        val result = repository(
            brewLogIds = listOf("brew-log-rejected"),
            outboxIds = listOf("outbox-brew-log-rejected"),
        ).createBrewLog(BrewLogDraft(bean = SelectedBean.Optimistic("bean-opt-update")))

        assertIs<LocalBrewLogException.OptimisticBeanMissing>(result.exceptionOrNull())
        assertEquals(1L, count("bean_cache", "id = 'bean-opt-update' AND profile_id = 'profile-1'"))
        assertEquals(0L, count("brew_logs"))
        assertEquals(1L, count("sync_outbox"))
        assertEquals(0L, count("sync_outbox", "entity_type = 'brew_log'"))
    }

    @Test
    fun createFailsClosedWithoutCurrentProfileAndWritesNothing() = runTest {
        val result = repository(profileId = null).createBrewLog(BrewLogDraft(bean = SelectedBean.Existing(existingBean())))

        assertIs<LocalBrewLogException.ProfileRequired>(result.exceptionOrNull())
        assertNoRows()
    }

    @Test
    fun createFailsClosedWithBlankCurrentProfileAndWritesNothing() = runTest {
        val result = repository(profileId = "  ").createBrewLog(BrewLogDraft(bean = SelectedBean.NewDraft(newBeanDraft())))

        assertIs<LocalBrewLogException.ProfileRequired>(result.exceptionOrNull())
        assertNoRows()
    }

    @Test
    fun createFailsClosedWithoutClientIdAndWritesNothing() = runTest {
        val result = repository(clientId = null).createBrewLog(BrewLogDraft(bean = SelectedBean.Existing(existingBean())))

        assertIs<LocalBrewLogException.ClientIdRequired>(result.exceptionOrNull())
        assertNoRows()
    }

    @Test
    fun createFailsClosedWithBlankClientIdAndWritesNothing() = runTest {
        val result = repository(clientId = " ").createBrewLog(BrewLogDraft(bean = SelectedBean.NewDraft(newBeanDraft())))

        assertIs<LocalBrewLogException.ClientIdRequired>(result.exceptionOrNull())
        assertNoRows()
    }

    @Test
    fun unsafeOptimisticBeanSelectionsFailClosedAndWriteNothing() = runTest {
        assertIs<LocalBrewLogException.OptimisticBeanMissing>(
            repository().createBrewLog(BrewLogDraft(bean = SelectedBean.Optimistic("missing"))).exceptionOrNull()
        )
        assertNoRows()

        insertOptimisticBeanRow(id = "cross", profileId = "other-profile")
        insertBeanOutbox(id = "cross-outbox", profileId = "other-profile", beanId = "cross")
        assertIs<LocalBrewLogException.OptimisticBeanMissing>(
            repository().createBrewLog(BrewLogDraft(bean = SelectedBean.Optimistic("cross"))).exceptionOrNull()
        )
        assertEquals(0L, count("brew_logs"))
        assertEquals(1L, count("bean_cache"))
        assertEquals(1L, count("sync_outbox"))

        insertOptimisticBeanRow(id = "no-outbox", profileId = "profile-1")
        assertIs<LocalBrewLogException.OptimisticBeanMissing>(
            repository().createBrewLog(BrewLogDraft(bean = SelectedBean.Optimistic("no-outbox"))).exceptionOrNull()
        )
        assertEquals(0L, count("brew_logs"))
    }

    @Test
    fun duplicateGeneratedIdsRollbackFailedCreateWithoutPartialRows() = runTest {
        repository(
            brewLogIds = listOf("duplicate-brew"),
            beanIds = listOf("bean-before"),
            outboxIds = listOf("outbox-before"),
        ).createBrewLog(BrewLogDraft(bean = SelectedBean.Existing(existingBean())))
        val failingRepository = repository(
            brewLogIds = listOf("duplicate-brew"),
            beanIds = listOf("bean-rollback"),
            outboxIds = listOf("outbox-bean-rollback", "outbox-brew-rollback"),
        )

        val result = failingRepository.createBrewLog(BrewLogDraft(bean = SelectedBean.NewDraft(newBeanDraft(name = "Rollback Bean"))))

        assertIs<LocalBrewLogException.PersistenceFailed>(result.exceptionOrNull())
        assertEquals(1L, count("brew_logs"))
        assertEquals(0L, count("bean_cache", "id = 'bean-rollback'"))
        assertEquals(0L, count("sync_outbox", "entity_id = 'bean-rollback' OR id IN ('outbox-bean-rollback', 'outbox-brew-rollback')"))
    }

    @Test
    fun getBrewLogsReturnsCurrentProfileNonDeletedLocalProjectionsWithExplicitBeanRefs() = runTest {
        repository(
            brewLogIds = listOf("existing-log"),
            beanIds = listOf("unused"),
            outboxIds = listOf("outbox-existing"),
        ).createBrewLog(BrewLogDraft(bean = SelectedBean.Existing(existingBean())))
        repository(
            brewLogIds = listOf("optimistic-log"),
            beanIds = listOf("bean-opt-1"),
            outboxIds = listOf("outbox-bean-1", "outbox-optimistic"),
        ).createBrewLog(BrewLogDraft(bean = SelectedBean.NewDraft(newBeanDraft())))
        insertBrewLogRow(id = "deleted-log", profileId = "profile-1", beanId = "bean-1", deletedAt = 99)
        insertBrewLogRow(id = "noop-log", profileId = "profile-1", beanId = "bean-1", syncStatus = "no_op")
        insertBrewLogRow(id = "other-profile-log", profileId = "profile-2", beanId = "bean-1")

        val logs = repository().getBrewLogs().getOrThrow()

        assertEquals(setOf("existing-log", "optimistic-log"), logs.map { it.id }.toSet())
        assertTrue(logs.any { it.bean is LocalBeanRef.Existing })
        assertTrue(logs.any { it.bean is LocalBeanRef.Optimistic })
        assertTrue(logs.all { it.profileId == "profile-1" && it.deletedAtMillis == null && it.syncStatus != LocalSyncStatus.NoOp })
    }

    private fun TestScope.repository(
        profileId: String? = "profile-1",
        clientId: String? = "client-1",
        brewLogIds: List<String> = listOf("brew-log-1"),
        beanIds: List<String> = listOf("bean-opt-1"),
        outboxIds: List<String> = listOf("outbox-1", "outbox-2", "outbox-3"),
        nowMillis: Long = 1_717_171_717_000L,
    ): BrewLogRepository = SqlDelightBrewLogRepository(
        database = database,
        transactionDispatcher = TransactionDispatcher(StandardTestDispatcher(testScheduler)),
        currentProfileProvider = FakeCurrentProfileProvider(profileId),
        syncClientIdProvider = FakeSyncClientIdProvider(clientId),
        brewLogIdProvider = QueueBrewLogIdProvider(*brewLogIds.toTypedArray()),
        beanIdProvider = QueueBeanIdProvider(*beanIds.toTypedArray()),
        outboxIdProvider = QueueOutboxIdProvider(*outboxIds.toTypedArray()),
        epochMillisProvider = FakeEpochMillisProvider(nowMillis),
    )

    private fun existingBean(): Bean = Bean(
        id = "bean-1",
        name = "Finca Cupped",
        slug = "finca-cupped",
        country = "Colombia",
        region = "Huila",
        process = "washed",
        roastLevel = 3,
    )

    private fun newBeanDraft(name: String = "Yirgacheffe"): BeanDraft = BeanDraft(
        name = name,
        country = "Ethiopia",
        region = "Yirgacheffe",
        farm = "Farm A",
        producer = "Producer A",
        roaster = "Roaster A",
        process = "washed",
        roastLevel = 35,
    )

    private fun assertNoRows() {
        assertEquals(0L, count("brew_logs"))
        assertEquals(0L, count("bean_cache"))
        assertEquals(0L, count("sync_outbox"))
    }

    private fun insertOptimisticBeanRow(id: String, profileId: String) {
        driver.execute(null, """
            INSERT INTO bean_cache(id, profile_id, name, payload_json)
            VALUES (?, ?, 'Seed Bean', '{"schemaVersion":1,"id":"$id","name":"Seed Bean","createdAtMillis":1}')
        """.trimIndent(), 2) {
            bindString(0, id)
            bindString(1, profileId)
        }
    }

    private fun insertBeanOutbox(id: String, profileId: String, beanId: String, operation: String = "create") {
        driver.execute(null, """
            INSERT INTO sync_outbox(id, profile_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
            VALUES (?, ?, 'client-1', 'bean', ?, ?, 'pending', 1, 0)
        """.trimIndent(), 4) {
            bindString(0, id)
            bindString(1, profileId)
            bindString(2, beanId)
            bindString(3, operation)
        }
    }

    private fun insertBrewLogRow(
        id: String,
        profileId: String,
        beanId: String,
        syncStatus: String = "pending_create",
        deletedAt: Long? = null,
    ) {
        driver.execute(null, """
            INSERT INTO brew_logs(id, profile_id, bean_id, sync_status, local_revision, created_at, local_updated_at, deleted_at)
            VALUES (?, ?, ?, ?, 1, 1, 1, ?)
        """.trimIndent(), 5) {
            bindString(0, id)
            bindString(1, profileId)
            bindString(2, beanId)
            bindString(3, syncStatus)
            bindLong(4, deletedAt)
        }
    }

    private fun count(table: String, where: String? = null): Long =
        longValue("SELECT COUNT(*) FROM $table" + (where?.let { " WHERE $it" } ?: ""))

    private fun longValue(sql: String): Long {
        var value = 0L
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0)!!.also { value = it })
            },
            parameters = 0,
        )
        return value
    }

    private fun stringValue(sql: String): String? {
        var value: String? = null
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getString(0).also { value = it })
            },
            parameters = 0,
        )
        return value
    }

    private class FakeCurrentProfileProvider(private val profileId: String?) : CurrentProfileProvider {
        override fun currentProfileId(): String? = profileId
    }

    private class FakeSyncClientIdProvider(private val clientId: String?) : SyncClientIdProvider {
        override fun clientId(): String? = clientId
    }

    private class QueueBrewLogIdProvider(private vararg val ids: String) : BrewLogIdProvider {
        private var index = 0
        override fun nextBrewLogId(): String = ids[index++]
    }

    private class QueueBeanIdProvider(private vararg val ids: String) : BeanIdProvider {
        private var index = 0
        override fun nextBeanId(): String = ids[index++]
    }

    private class QueueOutboxIdProvider(private vararg val ids: String) : OutboxIdProvider {
        private var index = 0
        override fun nextOutboxId(entityType: String, entityId: String, operation: String): String = ids[index++]
    }

    private class FakeEpochMillisProvider(private val nowMillis: Long) : EpochMillisProvider {
        override fun nowMillis(): Long = nowMillis
    }
}
