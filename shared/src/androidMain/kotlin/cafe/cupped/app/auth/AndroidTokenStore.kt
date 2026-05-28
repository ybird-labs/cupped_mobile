package cafe.cupped.app.auth

import android.content.Context
import cafe.cupped.app.platform.KeystoreSecretStore

/**
 * Android [TokenStore] backed by [KeystoreSecretStore], which performs
 * AES-256-GCM envelope encryption with a key held in the Android Keystore.
 * Satisfies architecture §11: a persistent, platform-keystore-backed token
 * store.
 *
 * The stored representation is unchanged from the previous implementation:
 * the access token under [KEY_ACCESS] and the refresh token under
 * [KEY_REFRESH], with
 * [getTokens] falling back to the access token when no refresh token is
 * present.
 */
class AndroidTokenStore(context: Context) : TokenStore {

    private val secrets = KeystoreSecretStore(context)

    override fun getTokens(): StoredTokens? {
        val access = secrets.get(KEY_ACCESS) ?: return null
        val refresh = secrets.get(KEY_REFRESH) ?: access
        return StoredTokens(accessToken = access, refreshToken = refresh)
    }

    override fun saveTokens(tokens: StoredTokens) {
        secrets.put(KEY_ACCESS, tokens.accessToken)
        secrets.put(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        secrets.clear()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
