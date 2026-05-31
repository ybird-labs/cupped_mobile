package cafe.cupped.app.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Migration / schema harness backstop (architecture §17 step 3).
 *
 * `verifySqlDelightMigration` (Gradle, under `check`) validates that the
 * committed `.sqm` migrations plus CREATE statements reproduce the schema
 * snapshot in src/commonMain/sqldelight/databases. The research notes that task
 * has a false-negative history (#1541/#4138/#3378), so this test exercises the
 * schema against a real (JVM JDBC) SQLite engine as an explicit backstop.
 *
 * As new schema versions land, add a case here that opens an old-version DB and
 * runs `CuppedDatabase.Schema.migrate(driver, old, new)`, then asserts the
 * resulting tables. This verifies a clean create against real SQLite (which
 * `verifyMigrations` does not fully exercise) and pins the current schema
 * version.
 */
class SchemaMigrationTest {

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
    fun schemaCreatesCleanlyOnRealSqlite() {
        // Sanity: the partial unique index and core tables exist.
        val tables = mutableListOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
            mapper = { cursor ->
                QueryResult.Value(
                    buildList { while (cursor.next().value) add(cursor.getString(0)!!) }
                        .also { tables.addAll(it) }
                )
            },
            parameters = 0,
        )

        listOf(
            "brew_logs",
            "brew_log_server_shadow",
            "sync_outbox",
            "sync_state",
            "brew_method_cache",
            "flavor_note_cache",
            "bean_cache",
            "recipe_cache",
        ).forEach { expected ->
            assertTrue(expected in tables, "expected table '$expected' missing; have $tables")
        }
    }

    @Test
    fun migratesVersionOneUserScopedDataToVersionTwoProfileScopedSchema() {
        val migrationDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            createVersionOneSchema(migrationDriver)
            seedVersionOneData(migrationDriver)

            CuppedDatabase.Schema.migrate(migrationDriver, oldVersion = 1, newVersion = 2)

            listOf(
                "brew_logs" to "profile-brew-log",
                "sync_outbox" to "profile-outbox",
                "brew_log_server_shadow" to "profile-shadow",
                "bean_cache" to "profile-bean",
                "recipe_cache" to "profile-recipe",
            ).forEach { (table, expectedProfileId) ->
                val columns = tableColumns(migrationDriver, table)
                assertTrue("profile_id" in columns, "$table should have profile_id after migration: $columns")
                assertFalse("user_id" in columns, "$table should not retain user_id after migration: $columns")
                assertEquals(
                    expectedProfileId,
                    profileIdForSeededRow(migrationDriver, table),
                    "$table should copy existing user_id into profile_id",
                )
            }

            val outboxColumns = tableColumns(migrationDriver, "sync_outbox")
            assertTrue("depends_on_entity_type" in outboxColumns, "sync_outbox dependency type missing: $outboxColumns")
            assertTrue("depends_on_entity_id" in outboxColumns, "sync_outbox dependency id missing: $outboxColumns")
            assertEquals(null, stringValue(migrationDriver, "SELECT depends_on_entity_type FROM sync_outbox WHERE id = 'outbox-active'"))
            assertEquals(null, stringValue(migrationDriver, "SELECT depends_on_entity_id FROM sync_outbox WHERE id = 'outbox-active'"))

            val indexes = indexNames(migrationDriver)
            assertTrue("brew_logs_profile_idx" in indexes, "profile index missing after migration: $indexes")
            assertFalse("brew_logs_user_idx" in indexes, "legacy user index should be dropped: $indexes")
            assertEquals(
                listOf("profile_id"),
                indexColumns(migrationDriver, "brew_logs_profile_idx"),
                "brew_logs_profile_idx should index profile_id",
            )
            assertEquals(
                listOf("profile_id", "state", "next_attempt_at", "created_at"),
                indexColumns(migrationDriver, "sync_outbox_ready_idx"),
                "sync_outbox_ready_idx should be profile-scoped",
            )
            assertEquals(
                listOf("profile_id", "entity_type", "entity_id"),
                indexColumns(migrationDriver, "sync_outbox_entity_active_unique_idx"),
                "active outbox uniqueness should be profile-scoped",
            )

            insertVersionTwoOutbox(
                driver = migrationDriver,
                id = "outbox-same-entity-other-profile",
                profileId = "profile-other",
                entityId = "brew-log-v1",
                state = "pending",
            )
            assertFailsWith<Exception> {
                insertVersionTwoOutbox(
                    driver = migrationDriver,
                    id = "outbox-duplicate-active",
                    profileId = "profile-outbox",
                    entityId = "brew-log-v1",
                    state = "in_flight",
                )
            }
            insertVersionTwoOutbox(
                driver = migrationDriver,
                id = "outbox-fresh-after-dead-letter",
                profileId = "profile-dead-letter",
                entityId = "brew-log-dead-letter",
                state = "pending",
            )
        } finally {
            migrationDriver.close()
        }
    }

    @Test
    fun schemaVersionIsTwo() {
        assertEquals(2L, CuppedDatabase.Schema.version)
    }

    private fun createVersionOneSchema(driver: JdbcSqliteDriver) {
        listOf(
            """
                CREATE TABLE sync_state (
                    scope TEXT NOT NULL PRIMARY KEY,
                    cursor TEXT,
                    last_synced_at INTEGER
                )
            """,
            """
                CREATE TABLE brew_log_server_shadow (
                    id TEXT NOT NULL PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    server_version INTEGER NOT NULL,
                    server_updated_at INTEGER NOT NULL,
                    server_deleted_at INTEGER,
                    payload_json TEXT
                )
            """,
            """
                CREATE TABLE brew_method_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    slug TEXT NOT NULL
                )
            """,
            """
                CREATE TABLE flavor_note_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    slug TEXT NOT NULL,
                    category TEXT
                )
            """,
            """
                CREATE TABLE bean_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    user_id TEXT,
                    name TEXT NOT NULL,
                    slug TEXT,
                    country TEXT,
                    region TEXT,
                    process TEXT,
                    roast_level INTEGER,
                    payload_json TEXT
                )
            """,
            """
                CREATE TABLE recipe_cache (
                    id TEXT NOT NULL PRIMARY KEY,
                    user_id TEXT,
                    name TEXT NOT NULL,
                    grind_size INTEGER,
                    water_temp_f INTEGER,
                    ratio REAL,
                    payload_json TEXT
                )
            """,
            """
                CREATE TABLE sync_outbox (
                    id TEXT NOT NULL PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    client_id TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    base_server_version INTEGER,
                    state TEXT NOT NULL DEFAULT 'pending',
                    created_at INTEGER NOT NULL,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    next_attempt_at INTEGER,
                    last_error TEXT
                )
            """,
            """
                CREATE TABLE brew_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    bean_id TEXT,
                    bean_client_id TEXT,
                    bean_draft_json TEXT,
                    recipe_id TEXT,
                    recipe_client_id TEXT,
                    recipe_draft_json TEXT,
                    brew_method_id TEXT,
                    flavor_note_ids TEXT,
                    rating INTEGER,
                    notes TEXT,
                    latitude REAL,
                    longitude REAL,
                    location_name TEXT,
                    place_id TEXT,
                    logged_at_iso TEXT,
                    sync_status TEXT NOT NULL DEFAULT 'synced',
                    local_revision INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    local_updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    last_sync_error TEXT
                )
            """,
            "CREATE INDEX sync_outbox_ready_idx ON sync_outbox(user_id, state, next_attempt_at, created_at)",
            """
                CREATE UNIQUE INDEX sync_outbox_entity_active_unique_idx
                    ON sync_outbox(user_id, entity_type, entity_id)
                    WHERE state IN ('pending', 'in_flight', 'blocked_error', 'auth_blocked')
            """,
            "CREATE INDEX brew_logs_user_idx ON brew_logs(user_id)",
            "CREATE INDEX brew_logs_sync_status_idx ON brew_logs(sync_status)",
            "CREATE INDEX brew_logs_deleted_at_idx ON brew_logs(deleted_at)",
        ).forEach { sql ->
            driver.execute(identifier = null, sql = sql.trimIndent(), parameters = 0)
        }
    }

    private fun seedVersionOneData(driver: JdbcSqliteDriver) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO brew_logs
                  (id, user_id, bean_id, sync_status, local_revision, created_at, local_updated_at)
                VALUES ('brew-log-v1', 'profile-brew-log', 'bean-v1', 'pending_create', 1, 1000, 1000)
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO brew_log_server_shadow
                  (id, user_id, server_version, server_updated_at, payload_json)
                VALUES ('shadow-v1', 'profile-shadow', 7, 2000, '{}')
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO bean_cache
                  (id, user_id, name, payload_json)
                VALUES ('bean-v1', 'profile-bean', 'Migrated Bean', '{}')
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO recipe_cache
                  (id, user_id, name, payload_json)
                VALUES ('recipe-v1', 'profile-recipe', 'Migrated Recipe', '{}')
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO sync_outbox
                  (id, user_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
                VALUES ('outbox-active', 'profile-outbox', 'client-1', 'brew_log', 'brew-log-v1', 'create', 'pending', 3000, 0)
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO sync_outbox
                  (id, user_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
                VALUES ('outbox-dead-letter', 'profile-dead-letter', 'client-1', 'brew_log', 'brew-log-dead-letter', 'update', 'dead_letter', 3001, 0)
            """.trimIndent(),
            parameters = 0,
        )
    }

    private fun profileIdForSeededRow(driver: JdbcSqliteDriver, table: String): String? {
        val id = when (table) {
            "brew_logs" -> "brew-log-v1"
            "sync_outbox" -> "outbox-active"
            "brew_log_server_shadow" -> "shadow-v1"
            "bean_cache" -> "bean-v1"
            "recipe_cache" -> "recipe-v1"
            else -> error("Unexpected table: $table")
        }
        return stringValue(driver, "SELECT profile_id FROM $table WHERE id = '$id'")
    }

    private fun tableColumns(driver: JdbcSqliteDriver, table: String): List<String> =
        queryStrings(driver, "PRAGMA table_info($table)", column = 1)

    private fun indexNames(driver: JdbcSqliteDriver): List<String> = queryStrings(
        driver,
        "SELECT name FROM sqlite_master WHERE type='index' ORDER BY name",
        column = 0,
    )

    private fun indexColumns(driver: JdbcSqliteDriver, index: String): List<String> =
        queryStrings(driver, "PRAGMA index_info($index)", column = 2)

    private fun queryStrings(driver: JdbcSqliteDriver, sql: String, column: Int): List<String> {
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

    private fun stringValue(driver: JdbcSqliteDriver, sql: String): String? {
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

    private fun insertVersionTwoOutbox(
        driver: JdbcSqliteDriver,
        id: String,
        profileId: String,
        entityId: String,
        state: String,
    ) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO sync_outbox
                  (id, profile_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
                VALUES (?, ?, 'client-1', 'brew_log', ?, 'update', ?, 4000, 0)
            """.trimIndent(),
            parameters = 4,
        ) {
            bindString(0, id)
            bindString(1, profileId)
            bindString(2, entityId)
            bindString(3, state)
        }
    }
}
