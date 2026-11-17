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
     * Restores a saved session by rotating its refresh token.
     * A failed refresh clears the unusable local session.
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
                SupabaseSessionManager.clear(appContext)
                onResult(Result(false, "Saved session expired. Please sign in again."))
                return@refreshSession
            }

            val accessToken = authResult.accessToken
            val refreshToken = authResult.refreshToken

            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                SupabaseSessionManager.clear(appContext)
                onResult(Result(false, "Session refresh returned incomplete tokens. Please sign in again."))
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
                SupabaseSessionManager.clear(appContext)
                onResult(
                    Result(
                        success = false,
                        message = "Could not save refreshed session: ${e.message ?: "Unknown error"}"
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
