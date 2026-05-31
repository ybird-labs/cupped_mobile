package cafe.cupped.app.brewlog.domain

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RED tests for the local-first use-case seam.
 *
 * The current server-first use case rejects [SelectedBean.NewDraft]. Local-first
 * behavior should accept it and delegate to the data layer so the repository can
 * create an optimistic bean and pending brew log atomically.
 */
class SubmitBrewLogUseCaseLocalFirstRedTest {

    @Test
    fun newBeanDraftIsAcceptedAndDelegatedForOptimisticLocalCreate() = runTest {
        val repository = RecordingBrewLogRepository()
        val useCase = SubmitBrewLogUseCase(repository)
        val draft = BrewLogDraft(
            bean = SelectedBean.NewDraft(
                BeanDraft(
                    name = "Yirgacheffe",
                    country = "Ethiopia",
                    region = "Yirgacheffe",
                    farm = "Farm A",
                    producer = "Producer A",
                    roaster = "Roaster A",
                    process = "washed",
                    roastLevel = 35,
                )
            ),
            notes = "sweet citrus",
        )

        val result = useCase(draft)

        assertTrue(
            result.isSuccess,
            "local-first submit should accept new bean drafts; failure was ${result.exceptionOrNull()?.message}",
        )
        assertEquals(draft, repository.createdDraft, "new bean draft should reach repository unchanged")
        val selected = repository.createdDraft?.bean as? SelectedBean.NewDraft
        assertNotNull(selected, "repository should receive the NewDraft bean selection")
        assertEquals("Farm A", selected.draft.farm, "local-only extras must not be dropped at use-case boundary")
        assertEquals("Producer A", selected.draft.producer)
        assertEquals("Roaster A", selected.draft.roaster)
    }

    private class RecordingBrewLogRepository : BrewLogRepository {
        var createdDraft: BrewLogDraft? = null
            private set

        override suspend fun getOptions(): Result<BrewLogOptions> = Result.success(BrewLogOptions())

        override suspend fun createBrewLog(draft: BrewLogDraft): Result<BrewLog> {
            createdDraft = draft
            return Result.success(
                BrewLog(
                    id = "brew-log-1",
                    bean = Bean(
                        id = "bean-optimistic-1",
                        name = "Yirgacheffe",
                        slug = null,
                        country = "Ethiopia",
                        region = "Yirgacheffe",
                        process = "washed",
                        roastLevel = 35,
                    ),
                    notes = draft.notes,
                )
            )
        }

        override suspend fun getBrewLogs(): Result<List<BrewLog>> = Result.success(emptyList())
    }
}
