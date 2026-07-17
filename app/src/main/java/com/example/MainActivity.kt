package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StallViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StallViewModel = viewModel()
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            
            MyApplicationTheme(themeType = selectedTheme) {
                StallblickApp(viewModel)
            }
        }
    }
}

@Composable
fun StallblickApp(viewModel: StallViewModel) {
    val navController = rememberNavController()

    val activeAlert by viewModel.activeAlert.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val navItems = listOf(
                    NavItem("dashboard", "KI-Wache", Icons.Default.Dns, Icons.Outlined.Dns),
                    NavItem("herd", "Herde", Icons.Default.Pets, Icons.Outlined.Pets),
                    NavItem("diagnose", "KI-Diagnose", Icons.Default.Psychology, Icons.Outlined.Psychology),
                    NavItem("chat", "Assistent", Icons.Default.SmartToy, Icons.Outlined.SmartToy),
                    NavItem("settings", "Konfig.", Icons.Default.Settings, Icons.Outlined.Settings)
                )

                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${item.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel)
            }
            composable("herd") {
                HerdScreen(viewModel = viewModel)
            }
            composable("diagnose") {
                DiagnoseScreen(viewModel = viewModel)
            }
            composable("chat") {
                ChatScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // --- Urgent Calving Overlay ---
        val alert = activeAlert
        if (alert != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissActiveAlert() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Rote Warnung",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = if (alert.typ == "eskalation") "DRINGENDE ESKALATION!" else "SOFORT-ALARM: KALBUNG",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD32F2F)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = alert.nachricht,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (alert.typ == "eskalation") {
                                "Es liegt ein Komplikationsverdacht vor (über 60 Minuten Geburtsanzeichen ohne Fortschritt). Sofortige Kontrolle im Stall oder Kontaktierung des Tierarztes dringend empfohlen!"
                            } else {
                                "Die Austreibungsphase wurde erkannt. Bitte begebe dich sofort in den Stall (Oberer Stollenhof) zur Geburtsbegleitung."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissActiveAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.testTag("alert_confirm_btn")
                    ) {
                        Text("Ich gehe in den Stall")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.markEventResolved(alert.id) },
                        modifier = Modifier.testTag("alert_resolve_btn")
                    ) {
                        Text("Gelesen markieren")
                    }
                }
            )
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
