package cafe.cupped.app.db

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.IO is JVM-only; on Kotlin/Native the SQLiter native driver
// serializes all writes through a single connection, so Default is fine here.
actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
