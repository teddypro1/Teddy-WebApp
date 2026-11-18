package com.teddy.bpositive.ui.screens

import android.widget.Toast
import com.teddy.bpositive.data.SupabaseAuthRepository
import com.teddy.bpositive.data.SupabaseSessionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onAuthenticated: (SupabaseSessionManager.Session) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var isSignUp by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3459D6),
            secondary = Color(0xFF607CE5),
            background = Color(0xFFF3F5FB),
            surface = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Color(0xFF5377E8),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "T",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Teddy WebApp",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        if (isSignUp) "Create your account" else "Sign in to continue",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            message,
                            color = if (message.contains("success", ignoreCase = true) ||
                                message.contains("check your email", ignoreCase = true) ||
                                message.contains("created", ignoreCase = true)
                            ) {
                                Color(0xFF267443)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val cleanEmail = email.trim()
                            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                message = "Please enter a valid email address."
                                return@Button
                            }
                            if (password.length < 6) {
                                message = "Password must contain at least 6 characters."
                                return@Button
                            }

                            isLoading = true
                            message = ""

                            if (isSignUp) {
                                SupabaseAuthRepository.signUp(cleanEmail, password) { result ->
                                    isLoading = false
                                    message = if (result.success) {
                                        isSignUp = false
                                        password = ""
                                        "Account created. Check your email for the confirmation link, then sign in."
                                    } else {
                                        result.message
                                    }
                                }
                            } else {
                                SupabaseAuthRepository.signIn(
                                    context,
                                    cleanEmail,
                                    password
                                ) { result ->
                                    isLoading = false
                                    message = result.message
                                    if (result.success && result.session != null) {
                                        onAuthenticated(result.session)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isSignUp) "Create Account" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            password = ""
                            message = ""
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            if (isSignUp) {
                                "Already have an account? Sign In"
                            } else {
                                "New here? Create an account"
                            }
                        )
                    }
                }
            }
        }
    }
}
