package cafe.cupped.app.auth

/**
 * A pair of tokens used by the Ktor Bearer auth provider.
 *
 * The Brewer backend issues a single opaque bearer token (see
 * `POST /api/v1/auth/verify` and `POST /api/v1/auth/refresh` in the OpenAPI
 * spec). There is no separate OAuth-style refresh token: refresh is performed
 * by calling `/api/v1/auth/refresh` *with the current access token*, which
 * revokes existing tokens and issues a new one.
 *
 * To fit Ktor's [io.ktor.client.plugins.auth.providers.BearerTokens] model
 * (which expects both an access and a refresh token), we mirror the same
 * opaque token into both slots. [refreshToken] therefore equals
 * [accessToken] for this backend; it is kept as a distinct field so the
 * shape is forward-compatible if the backend later adds true refresh tokens.
 */
data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        /** Build tokens for a backend that issues a single opaque bearer token. */
        fun single(token: String): StoredTokens = StoredTokens(token, token)
    }
}

/**
 * Persistent, platform-keystore-backed store for the user's auth tokens.
 *
 * Implementations MUST persist across process restarts and SHOULD be backed
 * by the platform secure storage:
 *  - iOS: Keychain.
 *  - Android: Android Keystore-backed AES-256-GCM envelope encryption.
 *
 * All methods are synchronous and expected to be cheap; Ktor's `loadTokens`
 * / `refreshTokens` callbacks are suspend, but the store itself is keystore
 * I/O which is fast and is invoked off the main thread by the auth plugin.
 */
interface TokenStore {
    /** Returns the currently stored tokens, or null if the user is signed out. */
    fun getTokens(): StoredTokens?

    /** Persists [tokens], replacing any previously stored value. */
    fun saveTokens(tokens: StoredTokens)

    /** Removes all stored tokens (logout / account switch). */
    fun clear()
}
