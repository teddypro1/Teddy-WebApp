package com.teddy.bpositive.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only the serialized VaultEnvelope, never passwords or unwrapped master keys. */
object NotesVaultStorage {
    private const val PREFS = "teddy_vault_storage"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    sealed class LoadResult {
        data object Missing : LoadResult()
        data class Found(val envelope: NotesEncryption.VaultEnvelope) : LoadResult()
        data class Error(val message: String) : LoadResult()
    }

    @Synchronized
    fun save(context: Context, userId: String, envelope: NotesEncryption.VaultEnvelope): Boolean {
        require(userId.isNotBlank()) { "Account ID is missing." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(aliasFor(userId)))
        val ciphertext = cipher.doFinal(envelope.toJson().toByteArray(Charsets.UTF_8))
        val record = JSONObject()
            .put("nonce", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .toString()
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(storageKey(userId), record).commit()
    }

    @Synchronized
    fun load(context: Context, userId: String): LoadResult {
        if (userId.isBlank()) return LoadResult.Error("Account ID is missing.")
        return try {
            val record = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(storageKey(userId), null) ?: return LoadResult.Missing
            val json = JSONObject(record)
            val nonce = Base64.decode(json.getString("nonce"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(json.getString("ciphertext"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getExistingKey(aliasFor(userId)), GCMParameterSpec(TAG_BITS, nonce))
            val plaintext = cipher.doFinal(ciphertext)
            val envelopeJson = try { String(plaintext, Charsets.UTF_8) } finally { plaintext.fill(0) }
            LoadResult.Found(NotesEncryption.VaultEnvelope.fromJson(envelopeJson))
        } catch (error: Exception) {
            LoadResult.Error(error.message ?: "Could not read local Vault data.")
        }
    }

    private fun aliasFor(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray(Charsets.UTF_8))
        val suffix = digest.joinToString("") { "%02x".format(it) }
        return "TeddyNotesVault_" + suffix
    }

    private fun storageKey(userId: String): String = "vault_" + aliasFor(userId)

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private fun getExistingKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
            ?: throw IllegalStateException("This device's Vault key is unavailable.")
    }
}
