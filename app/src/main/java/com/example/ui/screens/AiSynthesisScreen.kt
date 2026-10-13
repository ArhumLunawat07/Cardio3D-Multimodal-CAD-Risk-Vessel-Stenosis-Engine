package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VesselType
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiState
import com.example.ui.viewmodel.CardioViewModel
import com.example.ui.viewmodel.ImageGenState

@Composable
fun AiSynthesisScreen(
    viewModel: CardioViewModel,
    modifier: Modifier = Modifier
) {
    val patientData by viewModel.patientData.collectAsState()
    val assessment by viewModel.assessment.collectAsState()
    val deepThinkingState by viewModel.deepThinkingState.collectAsState()
    val guidelineSearchState by viewModel.guidelineSearchState.collectAsState()
    val fastTriageState by viewModel.fastTriageState.collectAsState()
    val imageGenState by viewModel.imageGenState.collectAsState()

    var customThinkingQuery by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("Latest ESC / ACC guidelines on chronic coronary syndromes and LAD revascularization") }
    var illustrationPrompt by remember { mutableStateOf("A 3D medical coronary angiogram rendering showing the Left Anterior Descending (LAD) coronary artery with 80% stenosis narrowing, contrast dye flow, high contrast medical visualization") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardioDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArterialRedPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Multimodal Cardiology Intelligence",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "Powered by Deep Thinking (gemini-3.1-pro-preview), Grounded Search (gemini-3.5-flash), and Low-Latency Triage (gemini-3.1-flash-lite)",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        // Section 1: Deep Thinking Mode (gemini-3.1-pro-preview, thinkingLevel HIGH)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ArterialRedPrimary.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(ArterialRedPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deep Thinking Analysis",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Surface(
                            color = ArterialRedPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "gemini-3.1-pro-preview (HIGH)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArterialRedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Synthesizes multi-organ physiology, ECG leads (V1-V6, inferior), troponin enzymes, and echo RWMA to reason deeply on CAD stenosis in LAD, LCX, and RCA.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = customThinkingQuery,
                        onValueChange = { customThinkingQuery = it },
                        placeholder = { Text("Optional clinician focus (e.g. 'Differentiate LAD vs RCA risk' or 'Evaluate PCI vs CABG')", fontSize = 11.sp, color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArterialRedPrimary,
                            unfocusedBorderColor = CardioBorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Button(
                        onClick = {
                            val q = if (customThinkingQuery.isBlank()) "Perform comprehensive cardiovascular risk synthesis and anatomical vessel correlation." else customThinkingQuery
                            viewModel.runDeepThinkingAnalysis(q)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArterialRedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_deep_thinking_button"),
                        enabled = deepThinkingState !is AiState.Loading
                    ) {
                        if (deepThinkingState is AiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deep Thinking in Progress (ThinkingLevel.HIGH)...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Execute Deep Thinking Synthesis", fontSize = 13.sp)
                        }
                    }

                    when (val state = deepThinkingState) {
                        is AiState.Success -> {
                            Surface(
                                color = CardioSurfaceVariantDark,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PerfusionHealthyEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cardiology Reasoning Report", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PerfusionHealthyEmerald)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.responseText,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                        is AiState.Error -> {
                            Surface(
                                color = ArterialRedGlow.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    fontSize = 11.sp,
                                    color = ArterialRedGlow,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // Section 2: Fast Low-Latency Emergency Triage (gemini-3.1-flash-lite)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VenousCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fast Bedside Triage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        Surface(
                            color = VenousCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "gemini-3.1-flash-lite",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VenousCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Generates instantaneous emergency classification and actionable bedside triage points.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    Button(
                        onClick = { viewModel.runFastTriage() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_fast_triage_btn"),
                        enabled = fastTriageState !is AiState.Loading
                    ) {
                        if (fastTriageState is AiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Triaging...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Instant Triage Protocol", fontSize = 12.sp)
                        }
                    }

                    when (val state = fastTriageState) {
                        is AiState.Success -> {
                            Surface(
                                color = CardioSurfaceVariantDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.responseText,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is AiState.Error -> {
                            Text(text = state.errorMessage, fontSize = 11.sp, color = ArterialRedGlow)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Section 3: Search Grounding (gemini-3.5-flash with googleSearch tool)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = PerfusionHealthyEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guidelines Search Grounding", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        Surface(
                            color = PerfusionHealthyEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "gemini-3.5-flash + Google Search",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PerfusionHealthyEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Queries Google Search for up-to-date ESC, AHA, ACC guidelines, landmark clinical trials, and pharmacology protocols.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PerfusionHealthyEmerald,
                            unfocusedBorderColor = CardioBorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Button(
                        onClick = { viewModel.searchGuidelines(searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = PerfusionHealthyEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_guidelines_btn"),
                        enabled = guidelineSearchState !is AiState.Loading
                    ) {
                        if (guidelineSearchState is AiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Searching Medical Guidelines...", fontSize = 12.sp, color = Color.Black)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Guidelines & Trials", fontSize = 12.sp, color = Color.Black)
                        }
                    }

                    when (val state = guidelineSearchState) {
                        is AiState.Success -> {
                            Surface(
                                color = CardioSurfaceVariantDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.responseText,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is AiState.Error -> {
                            Text(text = state.errorMessage, fontSize = 11.sp, color = ArterialRedGlow)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Section 4: Angiogram & Medical Illustration Generator (gemini-nano-banana-2.1)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = ArterialWarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Angiogram Illustration Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        Surface(
                            color = ArterialWarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "gemini-nano-banana-2.1",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArterialWarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Generates visual diagnostic representations or patient education diagrams from clinical prompts.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = illustrationPrompt,
                        onValueChange = { illustrationPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArterialWarningAmber,
                            unfocusedBorderColor = CardioBorderDark,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Button(
                        onClick = { viewModel.generateIllustration(illustrationPrompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = CardioSurfaceHighlight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_image_btn"),
                        enabled = imageGenState !is ImageGenState.Loading
                    ) {
                        if (imageGenState is ImageGenState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VenousCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rendering Image with Gemini...", fontSize = 12.sp, color = VenousCyan)
                        } else {
                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp), tint = VenousCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Angiogram Illustration", fontSize = 12.sp, color = VenousCyan)
                        }
                    }

                    when (val state = imageGenState) {
                        is ImageGenState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardioDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = state.bitmap.asImageBitmap(),
                                    contentDescription = "Generated Coronary Angiogram",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is ImageGenState.Error -> {
                            Text(text = state.errorMessage, fontSize = 11.sp, color = ArterialRedGlow)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
