package cafe.cupped.app.db

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The platform IO-style dispatcher used for database transactions in
 * production. Tests inject a `TestDispatcher` instead (architecture §10/§18).
 */
expect fun ioDispatcher(): CoroutineDispatcher
