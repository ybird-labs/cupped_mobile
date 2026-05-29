package cafe.cupped.app.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Deterministic JVM/common tests for the shared SQLCipher key helpers and the
 * [DatabaseKeyProvider] get-or-create / rotate contract.
 *
 * The real platform providers need a Context (Android Keystore) or the iOS
 * Keychain, which are device/Robolectric-only. These tests cover the
 * platform-independent logic: the raw-key format both platforms hand SQLCipher,
 * and the get-or-create/rotate contract via an in-memory provider that mirrors
 * the production providers' shape (read-then-generate-then-persist).
 */
class SqlCipherKeyTest {

    @Test
    fun rawKeyFormatWrapsHexInSqlCipherRawSyntax() {
        val hex = "a".repeat(64)
        assertEquals("x'${hex}'", rawKeyFormat(hex))
    }

    @Test
    fun rawKeyFormatRejectsNon64CharOrNonHex() {
        assertFailsWith<IllegalArgumentException> { rawKeyFormat("abc") }
        assertFailsWith<IllegalArgumentException> { rawKeyFormat("z".repeat(64)) }
    }

    @Test
    fun toHexKeyProduces64LowercaseHexCharsFor32Bytes() {
        val bytes = ByteArray(32) { 0xAB.toByte() }
        val hex = bytes.toHexKey()
        assertEquals(64, hex.length)
        assertEquals("ab".repeat(32), hex)
        // round-trips through the raw-key formatter
        assertTrue(rawKeyFormat(hex).startsWith("x'"))
    }

    @Test
    fun getOrCreateKeyIsStableAndRotateChangesIt() {
        val provider = InMemoryKeyProvider()
        val first = provider.getOrCreateKey()
        val second = provider.getOrCreateKey()
        assertEquals(first, second, "getOrCreateKey must return the same value on second call")

        provider.rotateKey()
        val afterRotate = provider.getOrCreateKey()
        assertNotEquals(first, afterRotate, "rotateKey must yield a different key")
        // and it must be a valid SQLCipher key
        rawKeyFormat(afterRotate)
    }
}

/** In-memory provider mirroring the production read/generate/persist contract. */
private class InMemoryKeyProvider : DatabaseKeyProvider {
    private var stored: String? = null
    private var counter = 0

    override fun getOrCreateKey(): String {
        stored?.let { return it }
        // Deterministic distinct keys per generation (no real CSPRNG needed here).
        val seed = (counter++).toByte()
        val generated = ByteArray(32) { (seed + it).toByte() }.toHexKey()
        stored = generated
        return generated
    }

    override fun rotateKey() {
        stored = null
    }
}
