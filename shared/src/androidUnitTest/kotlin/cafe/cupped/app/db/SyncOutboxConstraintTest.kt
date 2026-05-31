package cafe.cupped.app.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Enforces the `sync_outbox_entity_active_unique_idx` partial unique index
 * (architecture §5.3): at most one ACTIVE outbox row per (profile, entity_type,
 * entity_id), where ACTIVE = state IN (pending|in_flight|blocked_error|
 * auth_blocked). A `dead_letter` row is EXCLUDED so a dead-lettered item does
 * not block a fresh edit.
 *
 * Runs against a real (JVM JDBC) SQLite engine — the same backstop harness as
 * [SchemaMigrationTest].
 */
class SyncOutboxConstraintTest {

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

    private fun insertOutbox(id: String, state: String) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO sync_outbox
                  (id, profile_id, client_id, entity_type, entity_id, operation, state, created_at, retry_count)
                VALUES (?, 'profile_1', 'device_1', 'brew_log', 'brew_1', 'update', ?, 0, 0)
            """.trimIndent(),
            parameters = 2,
        ) {
            bindString(0, id)
            bindString(1, state)
        }
    }

    private fun countRows(): Long {
        var count = 0L
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM sync_outbox",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0)!!.also { count = it })
            },
            parameters = 0,
        )
        return count
    }

    @Test
    fun twoActiveRowsForSameEntityViolateUniqueIndex() {
        insertOutbox("m1", "pending")
        // A second ACTIVE row for the same (profile, entity_type, entity_id)
        // must fail specifically on the UNIQUE partial index — assert the
        // failure is a constraint violation, not some unrelated error.
        val ex = assertFailsWith<Exception> {
            insertOutbox("m2", "in_flight")
        }
        assertTrue(
            ex.message?.contains("UNIQUE", ignoreCase = true) == true ||
                ex.message?.contains("constraint", ignoreCase = true) == true,
            "expected a UNIQUE constraint violation, got: ${ex.message}",
        )
        assertEquals(1L, countRows(), "only the first active row should persist")
    }

    @Test
    fun deadLetterRowDoesNotConflictWithActiveRow() {
        insertOutbox("dead", "dead_letter")
        // dead_letter is excluded from the partial index, so a fresh active row
        // for the same entity is allowed alongside it.
        insertOutbox("fresh", "pending")
        assertEquals(2L, countRows(), "dead_letter must not block a fresh active row")
    }

    @Test
    fun multipleDeadLetterRowsAreAllowed() {
        insertOutbox("dead1", "dead_letter")
        insertOutbox("dead2", "dead_letter")
        assertTrue(countRows() == 2L)
    }
}
