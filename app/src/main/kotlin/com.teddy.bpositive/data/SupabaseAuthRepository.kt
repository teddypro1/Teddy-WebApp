package com.teddy.bpositive.data

import android.content.Context

/**
 * Coordinates Supabase Auth requests with encrypted local session storage.
 *
 * This is a backend/data-layer component only. It does not create or modify UI.
 */
object SupabaseAuthRepository {

    data class Result(
        val success: Boolean,
        val message: String,
        val session: SupabaseSessionManager.Session? = null
    )

    /**
     * Creates a Supabase account. When email confirmation is enabled,
     * Supabase may create the user without issuing a session yet.
     */
    fun signUp(
        email: String,
        password: String,
        onResult: (Result) -> Unit
    ) {
        SupabaseAuth.signUp(email, password) { authResult ->
            onResult(
                Result(
                    success = authResult.success,
                    message = authResult.message
                )
            )
        }
    }

    /**
     * Signs in and saves the returned tokens in encrypted session storage.
     */
    fun signIn(
        context: Context,
        email: String,
        password: String,
        onResult: (Result) -> Unit
    ) {
        SupabaseAuth.signIn(email, password) { authResult ->
            if (!authResult.success) {
                onResult(Result(false, authResult.message))
                return@signIn
            }

            val accessToken = authResult.accessToken
            val refreshToken = authResult.refreshToken

            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                onResult(
                    Result(
                        success = false,
                        message = "Login succeeded but Supabase did not return a complete session. Check email confirmation settings."
                    )
                )
                return@signIn
            }

            val session = SupabaseSessionManager.Session(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = authResult.userId,
                email = authResult.email ?: email.trim()
            )

            try {
                SupabaseSessionManager.save(context.applicationContext, session)
                onResult(
                    Result(
                        success = true,
                        message = "Signed in and session saved securely.",
                        session = session
                    )
                )
            } catch (e: Exception) {
                onResult(
                    Result(
                        success = false,
                        message = "Signed in, but secure session storage failed: ${e.message ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    /**
     * Restores a saved session. If the network is unavailable or Supabase is
     * temporarily unreachable, keep the encrypted local session so the user
     * can continue working with locally cached notes. Only a refresh-token
     * rejection that clearly indicates an invalid/revoked token clears it.
     */
    fun restoreSession(
        context: Context,
        onResult: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val savedSession = try {
            SupabaseSessionManager.load(appContext)
        } catch (e: Exception) {
            onResult(
                Result(
                    success = false,
                    message = "Could not read saved session: ${e.message ?: "Unknown error"}"
                )
            )
            return
        }

        if (savedSession == null) {
            onResult(Result(false, "No saved session."))
            return
        }

        SupabaseAuth.refreshSession(savedSession.refreshToken) { authResult ->
            if (!authResult.success) {
                val error = authResult.message.lowercase()
                val refreshTokenRejected =
                    error.contains("invalid refresh token") ||
                    error.contains("refresh_token_not_found") ||
                    error.contains("refresh token not found") ||
                    error.contains("refresh token is invalid") ||
                    error.contains("refresh token has been revoked")

                if (refreshTokenRejected) {
                    SupabaseSessionManager.clear(appContext)
                    onResult(
                        Result(
                            success = false,
                            message = "Your saved session is no longer valid. Please sign in again."
                        )
                    )
                } else {
                    // Network errors, timeouts and temporary server failures
                    // must not be treated as a sign-out.
                    onResult(
                        Result(
                            success = true,
                            message = "Offline mode: using your saved session. Cloud sync will resume when internet is available.",
                            session = savedSession
                        )
                    )
                }
                return@refreshSession
            }

            val accessToken = authResult.accessToken
            val refreshToken = authResult.refreshToken

            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                // Preserve the existing local session rather than locking the
                // user out because a refresh response was incomplete.
                onResult(
                    Result(
                        success = true,
                        message = "Using your saved session. Cloud session refresh was incomplete; it will be retried later.",
                        session = savedSession
                    )
                )
                return@refreshSession
            }

            val refreshedSession = SupabaseSessionManager.Session(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = authResult.userId ?: savedSession.userId,
                email = authResult.email ?: savedSession.email
            )

            try {
                SupabaseSessionManager.save(appContext, refreshedSession)
                onResult(
                    Result(
                        success = true,
                        message = "Session restored successfully.",
                        session = refreshedSession
                    )
                )
            } catch (e: Exception) {
                // The old encrypted session remains available if saving the
                // rotated token fails.
                onResult(
                    Result(
                        success = true,
                        message = "Using your saved session. Could not persist refreshed session: ${e.message ?: "Unknown error"}",
                        session = savedSession
                    )
                )
            }
        }
    }

    /**
     * Revokes the server session and clears local encrypted session data.
     */
    fun signOut(
        context: Context,
        onResult: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val session = SupabaseSessionManager.load(appContext)

        if (session == null) {
            SupabaseSessionManager.clear(appContext)
            onResult(Result(true, "No active session."))
            return
        }

        SupabaseAuth.signOut(session.accessToken) { authResult ->
            // Always clear local credentials so the app cannot keep using a
            // session after the user has explicitly requested sign-out.
            SupabaseSessionManager.clear(appContext)
            onResult(
                Result(
                    success = authResult.success,
                    message = if (authResult.success) {
                        "Signed out successfully."
                    } else {
                        "Local session cleared. Server sign-out response: ${authResult.message}"
                    }
                )
            )
        }
    }
}
