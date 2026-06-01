package cafe.cupped.app.brewlog.data

import cafe.cupped.app.bean.domain.BeanDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json

class LocalOptimisticBeanPayloadTest {

    @Test
    fun optimisticBeanPayloadRoundTripsLocalOnlyDraftFieldsWithoutDtos() {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val draft = BeanDraft(
            name = "Yirgacheffe",
            country = "Ethiopia",
            region = "Yirgacheffe",
            farm = "Farm A",
            producer = "Producer A",
            roaster = "Roaster A",
            process = "washed",
            roastLevel = 35,
        )
        val payload = LocalOptimisticBeanPayload.fromDraft(
            id = "bean-opt-1",
            draft = draft,
            createdAtMillis = 1_717_171_717_000L,
        )

        val encoded = json.encodeOptimisticBeanPayload(payload)
        val decoded = json.decodeOptimisticBeanPayload(encoded)

        assertEquals(1, decoded.schemaVersion)
        assertEquals("bean-opt-1", decoded.id)
        assertEquals(draft, decoded.toDraft())
        assertEquals("Farm A", decoded.farm)
        assertEquals("Producer A", decoded.producer)
        assertEquals("Roaster A", decoded.roaster)
        assertFalse(encoded.contains("BeanCreateRequest"), "local payload must not encode generated DTO names")
        assertFalse(encoded.contains("BrewLogCreateRequest"), "local payload must not encode generated DTO names")
    }
}
