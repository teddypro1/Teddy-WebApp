package com.teddy.bpositive.ui.screens
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import com.teddy.bpositive.data.SupabaseProfilesApi
import com.teddy.bpositive.ui.components.ProfileInfoRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ProfilePage(
    profile: SupabaseProfilesApi.Profile?,
    email: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    canEdit: Boolean = true,
    onEdit: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 18.dp
        val topInset = contentPadding.calculateTopPadding()
        val bottomInset = contentPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(
                    start = horizontalPadding,
                    top = topInset + 22.dp,
                    end = horizontalPadding,
                    bottom = bottomInset + 22.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            profile?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "👤",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatar ->
                            AsyncImage(
                                model = avatar,
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        profile?.fullName?.ifBlank { "Your Name" } ?: "Loading profile...",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Username not set",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        softWrap = true
                    )
                    if (!profile?.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            profile.bio.orEmpty(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            softWrap = true
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onEdit,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (canEdit) "Edit Profile" else "Profile editing requires internet")
                    }
                }
            }

            Text("Personal Information", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileInfoRow("Full Name", profile?.fullName.orEmpty())
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileInfoRow(
                        "Username",
                        profile?.username?.let { "@$it" }.orEmpty().ifBlank { "Not set" }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileInfoRow("Email", email)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileInfoRow(
                        "Account Created",
                        profile?.createdAt.orEmpty().ifBlank { "Unavailable" }
                    )
                }
            }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile Page")
@Composable
private fun ProfilePagePreview() {
    TeddyAppTheme(darkMode = false) {
        ProfilePage(
            profile = SupabaseProfilesApi.Profile(
                id = "preview-user", fullName = "Preview User", username = "preview_user",
                bio = "A sample profile for Compose Preview.", avatarUrl = null,
                createdAt = "2026-11-01T10:00:00Z", updatedAt = "2026-11-21T10:00:00Z"
            ),
            email = "preview@example.com", contentPadding = PaddingValues(0.dp),
            canEdit = true, onEdit = {}
        )
    }
}
