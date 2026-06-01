package cafe.cupped.app.brewlog.data

import cafe.cupped.app.bean.domain.BeanDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class LocalOptimisticBeanPayload(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val country: String? = null,
    val region: String? = null,
    val farm: String? = null,
    val producer: String? = null,
    val roaster: String? = null,
    val process: String? = null,
    val roastLevel: Int? = null,
    val createdAtMillis: Long,
) {
    fun toDraft(): BeanDraft = BeanDraft(
        name = name,
        country = country,
        region = region,
        farm = farm,
        producer = producer,
        roaster = roaster,
        process = process,
        roastLevel = roastLevel,
    )

    companion object {
        fun fromDraft(id: String, draft: BeanDraft, createdAtMillis: Long): LocalOptimisticBeanPayload =
            LocalOptimisticBeanPayload(
                id = id,
                name = draft.name,
                country = draft.country,
                region = draft.region,
                farm = draft.farm,
                producer = draft.producer,
                roaster = draft.roaster,
                process = draft.process,
                roastLevel = draft.roastLevel,
                createdAtMillis = createdAtMillis,
            )
    }
}

internal fun Json.encodeOptimisticBeanPayload(payload: LocalOptimisticBeanPayload): String =
    encodeToString(LocalOptimisticBeanPayload.serializer(), payload)

internal fun Json.decodeOptimisticBeanPayload(payloadJson: String): LocalOptimisticBeanPayload =
    decodeFromString(LocalOptimisticBeanPayload.serializer(), payloadJson)
