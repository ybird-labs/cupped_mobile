package cafe.cupped.app.auth

import cafe.cupped.app.platform.KeystoreSecretStore
import cafe.cupped.app.platform.SecretUndecryptableException
import io.github.aakira.napier.Napier

/**
 * Android [TokenStore] backed by the shared [KeystoreSecretStore], which
 * performs AES-256-GCM envelope encryption with a key held in the Android
 * Keystore. Satisfies architecture §11: a persistent, platform-keystore-backed
 * token store. The store is injected (a single instance shared with the DB key
 * provider) so key creation and the prefs file are not duplicated.
 *
 * The stored representation is unchanged: the access token under [KEY_ACCESS]
 * and the refresh token under [KEY_REFRESH], with [getTokens] falling back to
 * the access token when no refresh token is present.
 */
class AndroidTokenStore(private val secrets: KeystoreSecretStore) : TokenStore {

    override fun getTokens(): StoredTokens? {
        val access = try {
            secrets.get(KEY_ACCESS)
        } catch (e: SecretUndecryptableException) {
            // Token blob unreadable (Keystore key invalidated / corrupt) => treat
            // as signed out: drop the stale token entries and report no session.
            Napier.w("Stored tokens unreadable; clearing token entries -> signed out", e)
            clear()
            return null
        } ?: return null
        val refresh = try {
            secrets.get(KEY_REFRESH)
        } catch (e: SecretUndecryptableException) {
            null
        } ?: access
        return StoredTokens(accessToken = access, refreshToken = refresh)
    }

    override fun saveTokens(tokens: StoredTokens) {
        secrets.put(KEY_ACCESS, tokens.accessToken)
        secrets.put(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        // Remove ONLY the token entries. The shared store also holds the DB
        // passphrase (owned by AndroidDatabaseKeyProvider, wiped via the DB
        // logout/wipe flow) — clearing the whole store here would destroy it.
        secrets.remove(KEY_ACCESS)
        secrets.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
