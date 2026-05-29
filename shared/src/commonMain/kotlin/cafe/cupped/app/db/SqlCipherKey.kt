package cafe.cupped.app.db

/**
 * SQLCipher key helpers shared by both platforms (decision 2026-05-28).
 *
 * The DB key is a 256-bit CSPRNG secret, persisted in the platform keystore as
 * a lowercase hex string (64 hex chars). Both Android and iOS hand SQLCipher
 * the **raw key** format `x'<hex>'` — NOT a passphrase — so SQLCipher uses the
 * bytes directly with no PBKDF2 key derivation. Because the derivation is
 * identical (i.e. none) on both platforms, a DB written on one is readable on
 * the other.
 */

/** Number of hex characters in a valid 256-bit key (32 bytes * 2). */
private const val HEX_KEY_LENGTH = 64

/**
 * Wraps a 64-char lowercase-hex key in SQLCipher raw-key syntax `x'<hex>'`.
 * @throws IllegalArgumentException if [hexKey] is not exactly 64 hex chars.
 */
fun rawKeyFormat(hexKey: String): String {
    require(hexKey.length == HEX_KEY_LENGTH && hexKey.all { it in "0123456789abcdefABCDEF" }) {
        "SQLCipher key must be $HEX_KEY_LENGTH hex chars; got length=${hexKey.length}"
    }
    return "x'$hexKey'"
}

/** Encodes raw bytes to a lowercase hex string (key persistence format). */
fun ByteArray.toHexKey(): String =
    joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
