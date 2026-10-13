package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.CardioRiskEngine
import com.example.model.*
import com.example.ui.components.Heart3DCanvas
import com.example.ui.components.ThreeJsHeartView
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioViewModel
import kotlin.math.roundToInt

@Composable
fun VisualizerScreen(
    viewModel: CardioViewModel,
    onNavigateToInput: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToXai: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val patientData by viewModel.activePatient.collectAsState()
    val baselinePatient by viewModel.patientData.collectAsState()
    val assessment by viewModel.activeAssessment.collectAsState()
    val baselineAssessment by viewModel.assessment.collectAsState()
    val selectedVessel by viewModel.selectedVessel.collectAsState()
    val showRwmaOverlay by viewModel.showRwmaOverlay.collectAsState()
    val isThreeJsMode by viewModel.isThreeJsMode.collectAsState()
    val whatIfParams by viewModel.whatIfParams.collectAsState()
    val isWhatIfActive by viewModel.isWhatIfActive.collectAsState()
    val saveNotification by viewModel.saveNotification.collectAsState()

    var showWhatIfPanel by remember { mutableStateOf(false) }

    LaunchedEffect(saveNotification) {
        if (saveNotification != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearSaveNotification()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardioDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Patient Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CardioVision 3D",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (isWhatIfActive && whatIfParams.isModified) "What-If Lifestyle Simulation Mode" else "Anatomical Coronary Stenosis & Risk Mapping",
                                fontSize = 12.sp,
                                color = if (isWhatIfActive && whatIfParams.isModified) VenousCyan else TextSecondaryDark,
                                fontWeight = if (isWhatIfActive && whatIfParams.isModified) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // PDF Export Button
                            IconButton(
                                onClick = { viewModel.exportPdfReport(context) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(CardioSurfaceVariantDark, CircleShape)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export PDF Report",
                                    tint = ArterialRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Archive Record Button
                            IconButton(
                                onClick = { viewModel.saveCurrentAssessment() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(CardioSurfaceVariantDark, CircleShape)
                                    .testTag("save_assessment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Save Assessment",
                                    tint = VenousCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (saveNotification != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = PerfusionHealthyEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PerfusionHealthyEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = saveNotification ?: "",
                                fontSize = 11.sp,
                                color = PerfusionHealthyEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Clinical Case Presets Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Clinical Case Presets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(CardioRiskEngine.SAMPLE_PRESETS) { preset ->
                        val isCurrent = baselinePatient.name == preset.data.name
                        Surface(
                            onClick = { viewModel.loadPreset(preset) },
                            color = if (isCurrent) CardioSurfaceHighlight else CardioSurfaceDark,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) VenousCyan else CardioBorderDark
                            ),
                            modifier = Modifier.testTag("preset_${preset.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = preset.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (preset.badge.contains("Severe")) ArterialRedGlow else if (preset.badge.contains("Negative")) PerfusionHealthyEmerald else ArterialWarningAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3D Anatomical Heart Viewport (WebGL Three.js vs Native Canvas)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isThreeJsMode) "3D Heart (WebGL Three.js)" else "3D Heart (Vector Canvas)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (isThreeJsMode) VenousCyan else ElectricBlueAccent, CircleShape)
                        )
                    }

                    // 3D Engine Toggle
                    Surface(
                        onClick = { viewModel.toggleRendererMode() },
                        color = CardioSurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark),
                        modifier = Modifier.testTag("toggle_3d_engine_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = VenousCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isThreeJsMode) "Switch to Native" else "Switch to WebGL",
                                fontSize = 10.sp,
                                color = VenousCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3D Component Rendering
                if (isThreeJsMode) {
                    ThreeJsHeartView(
                        assessment = assessment,
                        pulseRate = patientData.pulseRate,
                        selectedVessel = selectedVessel,
                        onSelectVessel = { viewModel.selectVessel(it) },
                        showRwmaOverlay = showRwmaOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                } else {
                    Heart3DCanvas(
                        assessment = assessment,
                        pulseRate = patientData.pulseRate,
                        selectedVessel = selectedVessel,
                        onSelectVessel = { viewModel.selectVessel(it) },
                        showRwmaOverlay = showRwmaOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                }

                Text(
                    text = "Interactive 3D: Orbit touch controls, pinch zoom. Stenosed vessels (LAD/LCX/RCA $\\ge 50\\%$) pulse with critical red glow.",
                    fontSize = 10.sp,
                    color = TextMutedDark,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Live "What-If" Simulation Slider Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isWhatIfActive && whatIfParams.isModified) CardioSurfaceHighlight else CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isWhatIfActive && whatIfParams.isModified) VenousCyan else CardioBorderDark
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live 'What-If' Simulation Slider",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        IconButton(
                            onClick = { showWhatIfPanel = !showWhatIfPanel },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (showWhatIfPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextSecondaryDark
                            )
                        }
                    }

                    // Delta comparison badge
                    if (whatIfParams.isModified) {
                        val riskDiff = baselineAssessment.overallRiskPercent - assessment.overallRiskPercent
                        Surface(
                            color = PerfusionHealthyEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PerfusionHealthyEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Baseline: ${baselineAssessment.overallRiskPercent}% → Simulated: ${assessment.overallRiskPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (riskDiff >= 0) "-${riskDiff}% Risk Drop!" else "+${-riskDiff}% Risk",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PerfusionHealthyEmerald
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Simulate lifestyle tweaks (reducing sodium, aerobic cardio, statin therapy) and watch the 3D heart model and risk percentage update instantly.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    AnimatedVisibility(visible = showWhatIfPanel || whatIfParams.isModified) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Slider 1: Sodium Intake Reduction
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Dietary Sodium Reduction", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${whatIfParams.sodiumReductionPercent}% (Drops SBP ~${(whatIfParams.sodiumReductionPercent / 50f * 14).roundToInt()} mmHg)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VenousCyan)
                                }
                                Slider(
                                    value = whatIfParams.sodiumReductionPercent.toFloat(),
                                    onValueChange = { viewModel.updateWhatIf { p -> p.copy(sodiumReductionPercent = it.roundToInt()) } },
                                    valueRange = 0f..50f,
                                    colors = SliderDefaults.colors(thumbColor = VenousCyan, activeTrackColor = VenousCyan)
                                )
                            }

                            // Slider 2: Aerobic Cardio Hours / Week
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Aerobic Cardio Exercise", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${(whatIfParams.cardioHoursPerWeek * 10).roundToInt() / 10f} hrs/week", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PerfusionHealthyEmerald)
                                }
                                Slider(
                                    value = whatIfParams.cardioHoursPerWeek,
                                    onValueChange = { viewModel.updateWhatIf { p -> p.copy(cardioHoursPerWeek = (it * 10).roundToInt() / 10f) } },
                                    valueRange = 0f..7f,
                                    colors = SliderDefaults.colors(thumbColor = PerfusionHealthyEmerald, activeTrackColor = PerfusionHealthyEmerald)
                                )
                            }

                            // Statin Therapy Selector
                            Column {
                                Text("Statin Pharmacotherapy (Lipid Lowering)", fontSize = 11.sp, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatinIntensity.values().forEach { intensity ->
                                        val isSel = whatIfParams.statinTherapy == intensity
                                        Surface(
                                            onClick = { viewModel.updateWhatIf { p -> p.copy(statinTherapy = intensity) } },
                                            color = if (isSel) CardioSurfaceVariantDark else CardioSurfaceDark,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSel) VenousCyan else CardioBorderDark
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = when (intensity) {
                                                    StatinIntensity.NONE -> "None"
                                                    StatinIntensity.MODERATE -> "Moderate (-30%)"
                                                    StatinIntensity.HIGH_INTENSITY -> "High (-50%)"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) VenousCyan else TextSecondaryDark,
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Smoking Cessation Toggle
                            if (baselinePatient.smoker) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Smoking Cessation (Quit Tobacco)", fontSize = 11.sp, color = TextPrimaryDark)
                                    Switch(
                                        checked = whatIfParams.quitSmoking,
                                        onCheckedChange = { viewModel.updateWhatIf { p -> p.copy(quitSmoking = it) } }
                                    )
                                }
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetWhatIf() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                                ) {
                                    Text("Reset", fontSize = 11.sp, color = TextSecondaryDark)
                                }

                                Button(
                                    onClick = { viewModel.applyWhatIfToBaseline() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = VenousCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply to Baseline", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Target Vessels Status Matrix (LAD, LCX, RCA)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coronary Vessel Stenosis Predictions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Surface(
                            color = if (assessment.overallCadPositive) ArterialRedGlow.copy(alpha = 0.15f) else PerfusionHealthyEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (assessment.overallCadPositive) "CAD POSITIVE" else "CAD NEGATIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Three Vessel Rows
                    VesselType.values().forEach { vessel ->
                        val pred = assessment.vesselPredictions[vessel] ?: return@forEach
                        val isSelected = selectedVessel == vessel

                        Surface(
                            onClick = { viewModel.selectVessel(if (isSelected) null else vessel) },
                            color = if (isSelected) CardioSurfaceHighlight else CardioSurfaceVariantDark,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) VenousCyan else if (pred.isSignificantStenosis) ArterialRedGlow.copy(alpha = 0.4f) else CardioBorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = when {
                                                        pred.estimatedStenosisPercent >= 70 -> ArterialRedGlow
                                                        pred.estimatedStenosisPercent >= 50 -> ArterialRedPrimary
                                                        pred.estimatedStenosisPercent >= 30 -> ArterialWarningAmber
                                                        else -> PerfusionHealthyEmerald
                                                    },
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${vessel.code} (${vessel.fullName})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = vessel.territory,
                                                fontSize = 10.sp,
                                                color = TextMutedDark
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${pred.estimatedStenosisPercent}%",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (pred.isSignificantStenosis) ArterialRedGlow else PerfusionHealthyEmerald
                                        )
                                        Text(
                                            text = pred.severity.label,
                                            fontSize = 9.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { pred.estimatedStenosisPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = when {
                                        pred.estimatedStenosisPercent >= 70 -> ArterialRedGlow
                                        pred.estimatedStenosisPercent >= 50 -> ArterialRedPrimary
                                        pred.estimatedStenosisPercent >= 30 -> ArterialWarningAmber
                                        else -> PerfusionHealthyEmerald
                                    },
                                    trackColor = CardioSurfaceDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Navigation Buttons Row (XAI Breakdown, Clinical Vitals, AI Synthesis)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Clinical Actions & Deep Diagnostics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToXai,
                            colors = ButtonDefaults.buttonColors(containerColor = CardioSurfaceHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("nav_to_xai_btn")
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp), tint = VenousCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("XAI Breakdown", fontSize = 11.sp, color = VenousCyan)
                        }

                        Button(
                            onClick = { viewModel.exportPdfReport(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = ArterialRedPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("pdf_report_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export PDF", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
