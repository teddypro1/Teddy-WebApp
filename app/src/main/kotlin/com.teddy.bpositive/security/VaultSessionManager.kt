package com.teddy.bpositive.security

/**
 * Holds the unlocked Vault master key only in process memory.
 *
 * The manager never persists key material. Call lock() when the app leaves
 * the foreground or the signed-in account changes. Consumers receive copies
 * and must clear those copies after use.
 */
object VaultSessionManager {
    private var activeUserId: String? = null
    private var masterKey: ByteArray? = null

    @Synchronized
    fun unlock(userId: String, key: ByteArray) {
        require(userId.isNotBlank()) { "Account ID is missing." }
        require(key.size == 32) { "Invalid Vault master key." }

        clearKey()
        activeUserId = userId
        masterKey = key.copyOf()
    }

    @Synchronized
    fun isUnlocked(userId: String): Boolean {
        return userId.isNotBlank() &&
            activeUserId == userId &&
            masterKey?.size == 32
    }

    /**
     * Returns a defensive copy. The caller owns it and must zero it after use.
     */
    @Synchronized
    fun getMasterKeyCopy(userId: String): ByteArray? {
        if (!isUnlocked(userId)) return null
        return masterKey?.copyOf()
    }

    @Synchronized
    fun lock(userId: String? = null) {
        if (userId != null && activeUserId != userId) return
        clearKey()
        activeUserId = null
    }

    @Synchronized
    private fun clearKey() {
        masterKey?.fill(0)
        masterKey = null
    }
}
