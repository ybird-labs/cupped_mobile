package cafe.cupped.app.auth

import cafe.cupped.app.platform.IosKeychain

/**
 * iOS [TokenStore] backed by the Keychain (architecture §11), via [IosKeychain].
 */
class IosTokenStore(
    service: String = DEFAULT_SERVICE,
) : TokenStore {

    private val keychain = IosKeychain(service)

    override fun getTokens(): StoredTokens? {
        val access = keychain.read(KEY_ACCESS) ?: return null
        val refresh = keychain.read(KEY_REFRESH) ?: access
        return StoredTokens(accessToken = access, refreshToken = refresh)
    }

    override fun saveTokens(tokens: StoredTokens) {
        keychain.write(KEY_ACCESS, tokens.accessToken)
        keychain.write(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        keychain.delete(KEY_ACCESS)
        keychain.delete(KEY_REFRESH)
    }

    private companion object {
        const val DEFAULT_SERVICE = "cafe.cupped.app.auth.tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
