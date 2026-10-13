package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioViewModel
import com.example.xai.FeatureAttribution

@Composable
fun XaiDashboardScreen(
    viewModel: CardioViewModel,
    onNavigateToAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val patient by viewModel.activePatient.collectAsState()
    val assessment by viewModel.activeAssessment.collectAsState()
    val attributions by viewModel.featureAttributions.collectAsState()
    val rawEcgJson by viewModel.rawEcgJson.collectAsState()

    var ecgJsonText by remember(rawEcgJson) { mutableStateOf(rawEcgJson) }
    var isEcgExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardioDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "XAI Risk Breakdown Dashboard",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Surface(
                            color = CardioSurfaceVariantDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SHAP Feature Attribution",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VenousCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Parses multimodal biomarkers, hemodynamics, and ECG telemetry to quantify which specific marker is driving up the ${assessment.overallRiskPercent}% cardiac risk score.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        // Primary Driver Spotlight Banner
        val topDriver = attributions.firstOrNull { it.isPrimaryDriver }
        if (topDriver != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceHighlight),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ArterialRedGlow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(ArterialRedGlow.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = ArterialRedGlow, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#1 PRIMARY RISK DRIVER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArterialRedGlow
                            )
                            Text(
                                text = "${topDriver.featureName}: ${topDriver.currentValueDisplay}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Accounts for +${topDriver.percentageOfRisk}% of total excess cardiovascular risk. ${topDriver.clinicalMechanism}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }

        // Visual Contribution Chart (Horizontal Feature Attribution Waterfall/Bars)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Feature Contribution Waterfall Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Relative share of predicted cardiac risk contributed by individual clinical parameters:",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )

                    attributions.forEach { attr ->
                        val isPositive = attr.attributionPoints > 0
                        val barColor = if (!isPositive) {
                            PerfusionHealthyEmerald
                        } else if (attr.isPrimaryDriver) {
                            ArterialRedGlow
                        } else if (attr.attributionPoints >= 15f) {
                            ArterialRedPrimary
                        } else {
                            ArterialWarningAmber
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).background(barColor, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = attr.featureName,
                                        fontSize = 12.sp,
                                        fontWeight = if (attr.isPrimaryDriver) FontWeight.Bold else FontWeight.Medium,
                                        color = TextPrimaryDark
                                    )
                                }

                                Text(
                                    text = if (isPositive) "+${attr.percentageOfRisk}%" else "${attr.percentageOfRisk}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Horizontal Bar
                            val normalizedFraction = (kotlin.math.abs(attr.percentageOfRisk) / 35f).coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(CardioSurfaceVariantDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(normalizedFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(barColor)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current: ${attr.currentValueDisplay}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                                Text(
                                    text = "Reference: ${attr.referenceNormDisplay}",
                                    fontSize = 10.sp,
                                    color = TextMutedDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Raw 12-Lead ECG JSON Telemetry Inspector & Parser
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
                            Icon(Icons.Default.Code, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Raw 12-Lead ECG JSON Telemetry", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        IconButton(
                            onClick = { isEcgExpanded = !isEcgExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isEcgExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextSecondaryDark
                            )
                        }
                    }

                    Text(
                        text = "Inspect or edit raw serialized ECG telemetry JSON data. Parsing dynamically extracts ST-segment amplitudes and lead maps into the model:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )

                    if (isEcgExpanded) {
                        OutlinedTextField(
                            value = ecgJsonText,
                            onValueChange = { ecgJsonText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("ecg_json_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextPrimaryDark
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VenousCyan,
                                unfocusedBorderColor = CardioBorderDark,
                                focusedContainerColor = CardioDarkBackground,
                                unfocusedContainerColor = CardioDarkBackground
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { ecgJsonText = rawEcgJson },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                            ) {
                                Text("Reset JSON", fontSize = 11.sp, color = TextSecondaryDark)
                            }

                            Button(
                                onClick = { viewModel.updateRawEcgJson(ecgJsonText) },
                                modifier = Modifier.weight(1f).testTag("parse_ecg_json_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = VenousCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Parse & Apply", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            onClick = { isEcgExpanded = true },
                            color = CardioSurfaceVariantDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tap to view & edit structured ECG JSON (${patient.pulseRate} bpm, ${if (patient.stElevation) "${patient.stElevationLeads.size} ST leads" else "Normal ST"})", fontSize = 11.sp, color = VenousCyan)
                                Icon(Icons.Default.Edit, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
