package com.example.laburin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.laburin.R

enum class AppTab { SEARCH, APPLICATIONS, ACCOUNT }

@Composable
fun LaburinNavigationBar(
    selected: AppTab,
    isLoggedIn: Boolean,
    onSearchClick: () -> Unit,
    onApplicationsClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    val accountLabel = stringResource(if (isLoggedIn) R.string.nav_profile else R.string.nav_login)
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.6f))
        NavigationBar {
            NavigationBarItem(
                selected = selected == AppTab.SEARCH,
                onClick = onSearchClick,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_search)) },
            )
            NavigationBarItem(
                selected = selected == AppTab.APPLICATIONS,
                onClick = onApplicationsClick,
                icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_applications)) },
            )
            NavigationBarItem(
                selected = selected == AppTab.ACCOUNT,
                onClick = onAccountClick,
                icon = {
                    Icon(if (isLoggedIn) Icons.Default.Person else Icons.AutoMirrored.Filled.Login, contentDescription = null)
                },
                label = { Text(accountLabel) },
            )
        }
    }
}
