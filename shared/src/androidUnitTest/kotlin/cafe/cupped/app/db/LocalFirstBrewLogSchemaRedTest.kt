package cafe.cupped.app.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for the approved local-first brew-log Phase 1/2 schema
 * decisions and local durability invariants.
 */
class LocalFirstBrewLogSchemaRedTest {

    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuppedDatabase.Schema.create(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun localFirstSchemaUsesProfileIdInsteadOfUserId() {
        assertEquals(2L, CuppedDatabase.Schema.version, "local-first profile rename should land as schema v2")

        listOf(
            "brew_logs",
            "sync_outbox",
            "brew_log_server_shadow",
            "bean_cache",
            "recipe_cache",
        ).forEach { table ->
            val columns = tableColumns(table)
            assertTrue("profile_id" in columns, "$table should be scoped by profile_id: $columns")
            assertFalse("user_id" in columns, "$table should not expose legacy user_id after migration: $columns")
        }

        val indexes = indexNames()
        assertTrue("brew_logs_profile_idx" in indexes, "profile-named brew log index missing: $indexes")
        assertFalse("brew_logs_user_idx" in indexes, "legacy user-named brew log index should be gone: $indexes")
    }

    @Test
    fun syncOutboxSupportsProfileScopedUniquenessAndDependencyMetadata() {
        val columns = tableColumns("sync_outbox")
        assertTrue("profile_id" in columns, "sync_outbox must use profile_id: $columns")
        assertTrue("depends_on_entity_type" in columns, "sync_outbox needs dependency entity type: $columns")
        assertTrue("depends_on_entity_id" in columns, "sync_outbox needs dependency entity id: $columns")

        insertOutbox(
            id = "outbox-profile-1",
            profileId = "profile-1",
            entityType = "brew_log",
            entityId = "brew-log-1",
            operation = "create",
            state = "pending",
            dependsOnEntityType = "bean",
            dependsOnEntityId = "bean-optimistic-1",
        )
        insertOutbox(
            id = "outbox-profile-2",
            profileId = "profile-2",
            entityType = "brew_log",
            entityId = "brew-log-1",
            operation = "create",
            state = "pending",
        )

        assertEquals(
            2L,
            countRows("sync_outbox"),
            "same entity id in different profiles should not conflict",
        )
    }

    @Test
    fun optimisticBeanAndBrewLogCreateCanBePersistedWithDependencyRows() {
        insertOptimisticBean(
            id = "bean-optimistic-1",
            profileId = "profile-1",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "id": "bean-optimistic-1",
                  "name": "Yirgacheffe",
                  "localOnly": {"farm":"Farm A","producer":"Producer A","roaster":"Roaster A"}
                }
            """.trimIndent(),
        )
        insertBrewLog(
            id = "brew-log-1",
            profileId = "profile-1",
            beanId = "bean-optimistic-1",
        )
        insertOutbox(
            id = "outbox-bean-1",
            profileId = "profile-1",
            entityType = "bean",
            entityId = "bean-optimistic-1",
            operation = "create",
            state = "pending",
        )
        insertOutbox(
            id = "outbox-brew-log-1",
            profileId = "profile-1",
            entityType = "brew_log",
            entityId = "brew-log-1",
            operation = "create",
            state = "pending",
            dependsOnEntityType = "bean",
            dependsOnEntityId = "bean-optimistic-1",
        )

        assertEquals(1L, countRows("bean_cache"))
        assertEquals(1L, countRows("brew_logs"))
        assertEquals(2L, countRows("sync_outbox"))
        assertEquals(
            "bean:bean-optimistic-1",
            dependencyForOutbox("outbox-brew-log-1"),
            "brew-log create outbox row should depend on optimistic bean create",
        )
    }

    @Test
    fun existingBeanCreateCanBeRepresentedWithoutBeanDependency() {
        insertOptimisticBean(
            id = "bean-existing-1",
            profileId = "profile-1",
            payloadJson = "{\"schemaVersion\":1,\"id\":\"bean-existing-1\",\"name\":\"Existing\"}",
        )
        insertBrewLog(
            id = "brew-log-existing-bean-1",
            profileId = "profile-1",
            beanId = "bean-existing-1",
        )
        insertOutbox(
            id = "outbox-brew-existing-1",
            profileId = "profile-1",
            entityType = "brew_log",
            entityId = "brew-log-existing-bean-1",
            operation = "create",
            state = "pending",
        )

        assertEquals(
            null,
            dependencyForOutbox("outbox-brew-existing-1"),
            "existing/cached bean brew-log creates should not carry a bean dependency",
        )
    }

    private fun tableColumns(table: String): List<String> = queryStrings("PRAGMA table_info($table)", column = 1)

    private fun indexNames(): List<String> = queryStrings(
        "SELECT name FROM sqlite_master WHERE type='index' ORDER BY name",
        column = 0,
    )

    private fun queryStrings(sql: String, column: Int): List<String> {
        var values = emptyList<String>()
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) add(cursor.getString(column)!!)
                    }.also { values = it }
                )
            },
            parameters = 0,
        )
        return values
    }

    private fun insertOptimisticBean(id: String, profileId: String, payloadJson: String) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO bean_cache
                  (id, profile_id, name, slug, country, region, process, roast_level, payload_json)
                VALUES (?, ?, 'Yirgacheffe', NULL, 'Ethiopia', 'Yirgacheffe', 'washed', 35, ?)
            """.trimIndent(),
            parameters = 3,
        ) {
            bindString(0, id)
            bindString(1, profileId)
            bindString(2, payloadJson)
        }
    }

    private fun insertBrewLog(id: String, profileId: String, beanId: String) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO brew_logs
                  (id, profile_id, bean_id, sync_status, local_revision, created_at, local_updated_at)
                VALUES (?, ?, ?, 'pending_create', 1, 1000, 1000)
            """.trimIndent(),
            parameters = 3,
        ) {
            bindString(0, id)
            bindString(1, profileId)
            bindString(2, beanId)
        }
    }

    private fun insertOutbox(
        id: String,
        profileId: String,
        entityType: String,
        entityId: String,
        operation: String,
        state: String,
        dependsOnEntityType: String? = null,
        dependsOnEntityId: String? = null,
    ) {
        if (dependsOnEntityType == null && dependsOnEntityId == null) {
            driver.execute(
                identifier = null,
                sql = """
                    INSERT INTO sync_outbox
                      (id, profile_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
                    VALUES (?, ?, 'client-1', ?, ?, ?, ?, 1000, 0)
                """.trimIndent(),
                parameters = 6,
            ) {
                bindString(0, id)
                bindString(1, profileId)
                bindString(2, entityType)
                bindString(3, entityId)
                bindString(4, operation)
                bindString(5, state)
            }
        } else {
            driver.execute(
                identifier = null,
                sql = """
                    INSERT INTO sync_outbox
                      (id, profile_id, client_id, entity_type, entity_id, operation, state, created_at,
                       retry_count, depends_on_entity_type, depends_on_entity_id)
                    VALUES (?, ?, 'client-1', ?, ?, ?, ?, 1000, 0, ?, ?)
                """.trimIndent(),
                parameters = 8,
            ) {
                bindString(0, id)
                bindString(1, profileId)
                bindString(2, entityType)
                bindString(3, entityId)
                bindString(4, operation)
                bindString(5, state)
                bindString(6, dependsOnEntityType!!)
                bindString(7, dependsOnEntityId!!)
            }
        }
    }

    private fun countRows(table: String): Long {
        var count = 0L
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM $table",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0)!!.also { count = it })
            },
            parameters = 0,
        )
        return count
    }

    private fun dependencyForOutbox(outboxId: String): String? {
        var dependency: String? = null
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT depends_on_entity_type, depends_on_entity_id
                FROM sync_outbox
                WHERE id = ?
            """.trimIndent(),
            mapper = { cursor ->
                cursor.next()
                val type = cursor.getString(0)
                val id = cursor.getString(1)
                dependency = if (type == null && id == null) null else "$type:$id"
                QueryResult.Value(dependency)
            },
            parameters = 1,
        ) {
            bindString(0, outboxId)
        }
        return dependency
    }
}
