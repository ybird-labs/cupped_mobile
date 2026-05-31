package cafe.cupped.app.brewlog.domain

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * RED contract tests for the provider seams required by the approved ADR.
 *
 * These providers do not exist yet. They are required so local writes can be
 * profile-scoped and outbox rows can carry a stable per-install client id.
 */
class LocalFirstProviderContractsRedTest {

    @Test
    fun currentProfileProviderContractExistsForFailClosedLocalWrites() {
        assertNotNull(
            Class.forName("cafe.cupped.app.brewlog.domain.CurrentProfileProvider"),
            "CurrentProfileProvider should exist before local-first writes are implemented",
        )
    }

    @Test
    fun syncClientIdProviderContractExistsForOutboxRows() {
        assertNotNull(
            Class.forName("cafe.cupped.app.brewlog.domain.SyncClientIdProvider"),
            "SyncClientIdProvider should exist before outbox writes are implemented",
        )
    }
}
