package cafe.cupped.app.brewlog.domain

import cafe.cupped.app.bean.domain.Bean
import cafe.cupped.app.bean.domain.BeanDraft
import cafe.cupped.app.recipe.domain.RecipeDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SubmitBrewLogUseCaseLocalProjectionTest {

    @Test
    fun submitDelegatesExistingNewDraftAndOptimisticBeansReturningLocalBrewLog() = runTest {
        val repository = RecordingRepository()
        val useCase = SubmitBrewLogUseCase(repository)
        val drafts = listOf(
            BrewLogDraft(bean = SelectedBean.Existing(Bean(id = "bean-1", name = "Bean"))),
            BrewLogDraft(bean = SelectedBean.NewDraft(BeanDraft(name = "New Bean"))),
            BrewLogDraft(bean = SelectedBean.Optimistic("bean-opt-1")),
        )

        drafts.forEach { draft ->
            val result = useCase(draft)
            assertTrue(result.isSuccess)
            assertEquals("local-log", result.getOrThrow().id)
        }

        assertEquals(drafts, repository.createdDrafts)
    }

    @Test
    fun submitStillRejectsMissingBeanAndNewRecipeDraft() = runTest {
        val repository = RecordingRepository()
        val useCase = SubmitBrewLogUseCase(repository)

        val missingBean = useCase(BrewLogDraft())
        val recipeDraft = useCase(
            BrewLogDraft(
                bean = SelectedBean.Existing(Bean(id = "bean-1", name = "Bean")),
                recipe = SelectedRecipe.NewDraft(RecipeDraft(name = "Draft Recipe")),
            )
        )

        assertTrue(missingBean.isFailure)
        assertTrue(recipeDraft.isFailure)
        assertTrue(repository.createdDrafts.isEmpty(), "invalid drafts should not reach repository")
    }

    private class RecordingRepository : BrewLogRepository {
        val createdDrafts = mutableListOf<BrewLogDraft>()

        override suspend fun getOptions(): Result<BrewLogOptions> = Result.success(BrewLogOptions())

        override suspend fun createBrewLog(draft: BrewLogDraft): Result<LocalBrewLog> {
            createdDrafts += draft
            return Result.success(
                LocalBrewLog(
                    id = "local-log",
                    profileId = "profile-1",
                    bean = LocalBeanRef.Existing(Bean(id = "bean-1", name = "Bean")),
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
