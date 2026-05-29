package cafe.cupped.app.db

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The [CoroutineDispatcher] on which SQLDelight suspending transactions run.
 *
 * Injected (never hard-coded `Dispatchers.IO`) so invariant/failure-mode tests
 * can substitute a `TestDispatcher` and run in virtual time (architecture §10,
 * §18). Production wiring uses an IO-style dispatcher per platform.
 */
class TransactionDispatcher(val dispatcher: CoroutineDispatcher)
