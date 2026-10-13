package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioTab
import com.example.ui.viewmodel.CardioViewModel

class MainActivity : ComponentActivity() {

    private val cardioViewModel: CardioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = cardioViewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: CardioViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Handle back button: if on a sub-screen, return to 3D visualizer
    BackHandler(enabled = currentTab != CardioTab.VISUALIZER_3D) {
        viewModel.setTab(CardioTab.VISUALIZER_3D)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CardioDarkBackground),
        containerColor = CardioDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CardioSurfaceDark,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_bottom_nav_bar")
            ) {
                val items = listOf(
                    Triple(CardioTab.VISUALIZER_3D, "3D Heart", Icons.Default.ViewInAr),
                    Triple(CardioTab.CLINICAL_DATA, "Vitals", Icons.Default.Tune),
                    Triple(CardioTab.XAI_DASHBOARD, "XAI Charts", Icons.Default.Insights),
                    Triple(CardioTab.AI_CONSULTATION, "AI Synthesis", Icons.Default.AutoAwesome),
                    Triple(CardioTab.CHATBOT, "Consult Chat", Icons.Default.ChatBubble),
                    Triple(CardioTab.SAVED_RECORDS, "History", Icons.Default.History)
                )

                items.forEach { (tab, label, icon) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VenousCyan,
                            selectedTextColor = VenousCyan,
                            indicatorColor = CardioSurfaceHighlight,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                CardioTab.VISUALIZER_3D -> {
                    VisualizerScreen(
                        viewModel = viewModel,
                        onNavigateToInput = { viewModel.setTab(CardioTab.CLINICAL_DATA) },
                        onNavigateToAi = { viewModel.setTab(CardioTab.AI_CONSULTATION) },
                        onNavigateToXai = { viewModel.setTab(CardioTab.XAI_DASHBOARD) }
                    )
                }
                CardioTab.CLINICAL_DATA -> {
                    ClinicalDataScreen(
                        viewModel = viewModel,
                        onViewIn3D = { viewModel.setTab(CardioTab.VISUALIZER_3D) }
                    )
                }
                CardioTab.XAI_DASHBOARD -> {
                    XaiDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAi = { viewModel.setTab(CardioTab.AI_CONSULTATION) }
                    )
                }
                CardioTab.AI_CONSULTATION -> {
                    AiSynthesisScreen(
                        viewModel = viewModel
                    )
                }
                CardioTab.CHATBOT -> {
                    ChatScreen(
                        viewModel = viewModel
                    )
                }
                CardioTab.SAVED_RECORDS -> {
                    HistoryScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
