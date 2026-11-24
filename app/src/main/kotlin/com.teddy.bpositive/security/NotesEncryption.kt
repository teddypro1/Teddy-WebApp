package com.teddy.bpositive.security

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side cryptographic primitives for the notes vault.
 *
 * This class deliberately does not persist passwords, recovery keys, or master
 * keys. The caller must store only the serialized envelope and keep unlocked
 * key material in memory for as short a time as practical.
 */
object NotesEncryption {

    private const val VERSION = 1
    private const val AES_KEY_BYTES = 32
    private const val GCM_NONCE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val RECOVERY_KEY_BYTES = 32
    private const val PASSWORD_ITERATIONS = 310_000
    private const val RECOVERY_ITERATIONS = 120_000
    private const val KDF_BITS = 256

    private const val PASSWORD_AAD = "TeddyWebApp:master-key:password:v1"
    private const val RECOVERY_AAD = "TeddyWebApp:master-key:recovery:v1"
    private const val NOTE_AAD = "TeddyWebApp:note:v1"

    data class VaultEnvelope(
        val version: Int,
        val passwordSalt: String,
        val passwordNonce: String,
        val passwordWrappedMasterKey: String,
        val recoverySalt: String,
        val recoveryNonce: String,
        val recoveryWrappedMasterKey: String
    ) {
        fun toJson(): String = JSONObject()
            .put("version", version)
            .put("password_salt", passwordSalt)
            .put("password_nonce", passwordNonce)
            .put("password_wrapped_master_key", passwordWrappedMasterKey)
            .put("recovery_salt", recoverySalt)
            .put("recovery_nonce", recoveryNonce)
            .put("recovery_wrapped_master_key", recoveryWrappedMasterKey)
            .toString()

        companion object {
            fun fromJson(json: String): VaultEnvelope {
                val value = JSONObject(json)
                return VaultEnvelope(
                    version = value.getInt("version"),
                    passwordSalt = value.getString("password_salt"),
                    passwordNonce = value.getString("password_nonce"),
                    passwordWrappedMasterKey = value.getString("password_wrapped_master_key"),
                    recoverySalt = value.getString("recovery_salt"),
                    recoveryNonce = value.getString("recovery_nonce"),
                    recoveryWrappedMasterKey = value.getString("recovery_wrapped_master_key")
                ).also {
                    require(it.version == VERSION) { "Unsupported vault format version." }
                }
            }
        }
    }

    /** The recovery key is shown once during setup; never put it in the envelope. */
    data class VaultSetup(
        val envelope: VaultEnvelope,
        val recoveryKey: String,
        val masterKey: ByteArray
    )

    /** Creates a random master key and independently wraps it with password and recovery key. */
    fun createVault(password: CharArray): VaultSetup {
        require(password.size >= 12) { "Master password must contain at least 12 characters." }

        val masterKey = randomBytes(AES_KEY_BYTES)
        val recoveryKey = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(randomBytes(RECOVERY_KEY_BYTES))

        val passwordSalt = randomBytes(SALT_BYTES)
        val passwordKey = deriveKey(password, passwordSalt, PASSWORD_ITERATIONS)
        val passwordNonce = randomBytes(GCM_NONCE_BYTES)

        val recoverySalt = randomBytes(SALT_BYTES)
        val recoveryKeyBytes = deriveRecoveryKey(recoveryKey, recoverySalt)
        val recoveryNonce = randomBytes(GCM_NONCE_BYTES)

        return try {
            val envelope = VaultEnvelope(
                version = VERSION,
                passwordSalt = encode(passwordSalt),
                passwordNonce = encode(passwordNonce),
                passwordWrappedMasterKey = encode(
                    encryptAesGcm(passwordKey, passwordNonce, masterKey, PASSWORD_AAD)
                ),
                recoverySalt = encode(recoverySalt),
                recoveryNonce = encode(recoveryNonce),
                recoveryWrappedMasterKey = encode(
                    encryptAesGcm(recoveryKeyBytes, recoveryNonce, masterKey, RECOVERY_AAD)
                )
            )
            VaultSetup(envelope, recoveryKey, masterKey)
        } finally {
            passwordKey.fill(0)
            recoveryKeyBytes.fill(0)
        }
    }

    /** Unlocks the same master key using the user's master password. */
    fun unlockWithPassword(envelope: VaultEnvelope, password: CharArray): ByteArray {
        requireSupported(envelope)
        val key = deriveKey(password, decode(envelope.passwordSalt), PASSWORD_ITERATIONS)
        return try {
            decryptAesGcm(
                key,
                decode(envelope.passwordNonce),
                decode(envelope.passwordWrappedMasterKey),
                PASSWORD_AAD
            ).also { require(it.size == AES_KEY_BYTES) { "Invalid vault master key." } }
        } finally {
            key.fill(0)
        }
    }

    /** Unlocks the master key using the one-time-displayed recovery key. */
    fun unlockWithRecoveryKey(envelope: VaultEnvelope, recoveryKey: String): ByteArray {
        requireSupported(envelope)
        val key = deriveRecoveryKey(recoveryKey.trim(), decode(envelope.recoverySalt))
        return try {
            decryptAesGcm(
                key,
                decode(envelope.recoveryNonce),
                decode(envelope.recoveryWrappedMasterKey),
                RECOVERY_AAD
            ).also { require(it.size == AES_KEY_BYTES) { "Invalid vault master key." } }
        } finally {
            key.fill(0)
        }
    }

    /** Re-wraps an existing master key under a new password after recovery/unlock. */
    fun changePassword(
        envelope: VaultEnvelope,
        masterKey: ByteArray,
        newPassword: CharArray
    ): VaultEnvelope {
        requireSupported(envelope)
        require(masterKey.size == AES_KEY_BYTES) { "Invalid vault master key." }
        require(newPassword.size >= 12) { "Master password must contain at least 12 characters." }

        val salt = randomBytes(SALT_BYTES)
        val key = deriveKey(newPassword, salt, PASSWORD_ITERATIONS)
        val nonce = randomBytes(GCM_NONCE_BYTES)
        return try {
            envelope.copy(
                passwordSalt = encode(salt),
                passwordNonce = encode(nonce),
                passwordWrappedMasterKey = encode(
                    encryptAesGcm(key, nonce, masterKey, PASSWORD_AAD)
                )
            )
        } finally {
            key.fill(0)
        }
    }

    /** Encrypts one UTF-8 note field. A fresh random nonce is used for every call. */
    fun encryptNoteField(masterKey: ByteArray, plaintext: String): String {
        require(masterKey.size == AES_KEY_BYTES) { "Invalid vault master key." }
        val nonce = randomBytes(GCM_NONCE_BYTES)
        val ciphertext = encryptAesGcm(
            masterKey,
            nonce,
            plaintext.toByteArray(Charsets.UTF_8),
            NOTE_AAD
        )
        return "v1." + encode(nonce) + "." + encode(ciphertext)
    }

    /** Decrypts a field produced by encryptNoteField; authentication failures are propagated. */
    fun decryptNoteField(masterKey: ByteArray, encrypted: String): String {
        require(masterKey.size == AES_KEY_BYTES) { "Invalid vault master key." }
        val parts = encrypted.split('.')
        require(parts.size == 3 && parts[0] == "v1") { "Unsupported encrypted note format." }
        val plaintext = decryptAesGcm(
            masterKey,
            decode(parts[1]),
            decode(parts[2]),
            NOTE_AAD
        )
        return plaintext.toString(Charsets.UTF_8)
    }

    private fun requireSupported(envelope: VaultEnvelope) {
        require(envelope.version == VERSION) { "Unsupported vault format version." }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KDF_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun deriveRecoveryKey(recoveryKey: String, salt: ByteArray): ByteArray {
        require(recoveryKey.isNotBlank()) { "Recovery key cannot be empty." }
        val recoveryChars = recoveryKey.trim().toCharArray()
        return try {
            deriveKey(recoveryChars, salt, RECOVERY_ITERATIONS)
        } finally {
            recoveryChars.fill(0.toChar())
        }
    }

    private fun encryptAesGcm(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        aad: String
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(plaintext)
    }

    private fun decryptAesGcm(
        key: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray,
        aad: String
    ): ByteArray {
        require(nonce.size == GCM_NONCE_BYTES) { "Invalid encrypted data nonce." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also {
        SecureRandom().nextBytes(it)
    }

    private fun encode(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun decode(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)
}
