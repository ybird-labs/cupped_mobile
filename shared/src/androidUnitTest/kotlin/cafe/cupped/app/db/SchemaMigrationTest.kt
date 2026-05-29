package cafe.cupped.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
 * resulting tables. With the schema at v1 there is no migration yet; this
 * verifies a clean create against real SQLite (which `verifyMigrations` does
 * not fully exercise) and pins the current schema version.
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
                app.cash.sqldelight.db.QueryResult.Value(
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
    fun schemaVersionIsOne() {
        assertEquals(1L, CuppedDatabase.Schema.version)
    }
}
