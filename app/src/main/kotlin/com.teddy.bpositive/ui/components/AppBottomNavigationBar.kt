package com.teddy.bpositive.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    showAdmin: Boolean = false
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        (listOf(
            Triple("Home", "⌂", "Home"),
            Triple("Trash", "▤", "Trash"),
            Triple("Inbox", "♢", "Inbox"),
            Triple("Profile", "●", "Profile"),
            Triple("Settings", "⚙", "Settings")
        ) + if (showAdmin) listOf(Triple("Admin", "✦", "Admin")) else emptyList())
            .forEach { (tab, symbol, label) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Text(symbol, fontSize = 20.sp) },
                label = { Text(label, maxLines = 1) },
                alwaysShowLabel = true
            )
        }
    }
}
