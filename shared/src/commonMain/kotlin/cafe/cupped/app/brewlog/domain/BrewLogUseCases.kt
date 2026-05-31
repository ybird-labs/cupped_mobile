package cafe.cupped.app.brewlog.domain

// Loads picker/options data needed by the log-brew form.
class GetBrewLogOptionsUseCase(
    private val repository: BrewLogRepository
) {
    suspend operator fun invoke(): Result<BrewLogOptions> =
        repository.getOptions()
}

// Validates and submits a brew log draft.
class SubmitBrewLogUseCase(
    private val repository: BrewLogRepository
) {
    suspend operator fun invoke(draft: BrewLogDraft): Result<BrewLog> {
        if (draft.bean == null) {
            return Result.failure(
                IllegalArgumentException("Select a bean before submitting")
            )
        }

        if (draft.recipe is SelectedRecipe.NewDraft) {
            return Result.failure(
                IllegalArgumentException("Select an existing recipe or leave recipe empty before submitting")
            )
        }

        return repository.createBrewLog(draft)
    }
}
