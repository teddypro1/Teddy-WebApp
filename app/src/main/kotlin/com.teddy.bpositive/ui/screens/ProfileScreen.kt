package com.teddy.bpositive.ui.screens

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
fun ProfilePage(profile: SupabaseProfilesApi.Profile?, email: String, canEdit: Boolean = true, onEdit: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("My Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(104.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(profile?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "👤", fontSize = 38.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatar ->
                        AsyncImage(model = avatar, contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(profile?.fullName?.ifBlank { "Your Name" } ?: "Loading profile...", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Username not set",
                    color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if (!profile?.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(profile.bio.orEmpty(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onEdit, enabled = canEdit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(if (canEdit) "Edit Profile" else "Profile editing requires internet")
                }
            }
        }
        Text("Personal Information", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileInfoRow("Full Name", profile?.fullName.orEmpty())
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow("Username", profile?.username?.let { "@$it" }.orEmpty().ifBlank { "Not set" })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow("Email", email)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow("Account Created", profile?.createdAt.orEmpty().ifBlank { "Unavailable" })
            }
        }
    }
}
