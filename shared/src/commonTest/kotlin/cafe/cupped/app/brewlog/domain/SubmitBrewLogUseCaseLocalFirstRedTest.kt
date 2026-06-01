package cafe.cupped.app.brewlog.domain

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression coverage for the local-first use-case seam.
 *
 * [SelectedBean.NewDraft] is now accepted and delegated to the data layer so the
 * repository can create an optimistic bean and pending brew log atomically. Only
 * a missing bean and [SelectedRecipe.NewDraft] remain rejected at this boundary.
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

        override suspend fun createBrewLog(draft: BrewLogDraft): Result<LocalBrewLog> {
            createdDraft = draft
            return Result.success(
                LocalBrewLog(
                    id = "brew-log-1",
                    profileId = "profile-1",
                    bean = LocalBeanRef.Optimistic(
                        id = "bean-optimistic-1",
                        draft = (draft.bean as SelectedBean.NewDraft).draft,
                        syncState = LocalDependencySyncState.Pending,
                    ),
                    notes = draft.notes,
                    syncStatus = LocalSyncStatus.PendingCreate,
                    localRevision = 1,
                    createdAtMillis = 1,
                    localUpdatedAtMillis = 1,
                )
            )
        }

        override suspend fun getBrewLogs(): Result<List<LocalBrewLog>> = Result.success(emptyList())
    }
}
