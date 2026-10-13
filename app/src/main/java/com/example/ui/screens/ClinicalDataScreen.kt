package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.CardioRiskEngine
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalDataScreen(
    viewModel: CardioViewModel,
    onViewIn3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    val patientData by viewModel.patientData.collectAsState()
    val assessment by viewModel.assessment.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(CardioDarkBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Patient Clinical Parameters",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Real-time multimodal physiological & lab input",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = onViewIn3D,
                        modifier = Modifier
                            .background(CardioSurfaceDark, CircleShape)
                            .testTag("nav_to_3d_btn")
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "View 3D", tint = VenousCyan)
                    }
                }
            }

            // Quick Preset Loader
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Load Clinical Benchmark Case:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CardioRiskEngine.SAMPLE_PRESETS) { preset ->
                            val isSelected = patientData.name == preset.data.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.loadPreset(preset) },
                                label = { Text(preset.title.split(":").first(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CardioSurfaceHighlight,
                                    selectedLabelColor = VenousCyan,
                                    containerColor = CardioSurfaceDark,
                                    labelColor = TextSecondaryDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = CardioBorderDark,
                                    selectedBorderColor = VenousCyan
                                )
                            )
                        }
                    }
                }
            }

            // Section 1: Demographics & Anthropometry
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Demographics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        // Age Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Age", fontSize = 12.sp, color = TextSecondaryDark)
                                Text("${patientData.age} years", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                            Slider(
                                value = patientData.age.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(age = it.roundToInt()) } },
                                valueRange = 25f..90f,
                                colors = SliderDefaults.colors(thumbColor = VenousCyan, activeTrackColor = VenousCyan),
                                modifier = Modifier.testTag("slider_age")
                            )
                        }

                        // Sex & BMI Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Sex
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biological Sex", fontSize = 12.sp, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardioSurfaceVariantDark)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.updatePatient { it.copy(isMale = true) } }
                                            .background(if (patientData.isMale) CardioSurfaceHighlight else Color.Transparent)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Male", fontSize = 12.sp, fontWeight = if (patientData.isMale) FontWeight.Bold else FontWeight.Normal, color = if (patientData.isMale) VenousCyan else TextSecondaryDark)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.updatePatient { it.copy(isMale = false) } }
                                            .background(if (!patientData.isMale) CardioSurfaceHighlight else Color.Transparent)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Female", fontSize = 12.sp, fontWeight = if (!patientData.isMale) FontWeight.Bold else FontWeight.Normal, color = if (!patientData.isMale) VenousCyan else TextSecondaryDark)
                                    }
                                }
                            }

                            // BMI
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("BMI", fontSize = 12.sp, color = TextSecondaryDark)
                                    Text("${patientData.bmi}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Slider(
                                    value = patientData.bmi,
                                    onValueChange = { viewModel.updatePatient { p -> p.copy(bmi = (it * 10).roundToInt() / 10f) } },
                                    valueRange = 18f..42f,
                                    colors = SliderDefaults.colors(thumbColor = VenousCyan, activeTrackColor = VenousCyan)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Vitals & Hemodynamics
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = ArterialRedPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vital Signs & Hemodynamics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        // Systolic BP
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Systolic BP (mmHg)", fontSize = 12.sp, color = TextSecondaryDark)
                                Text("${patientData.systolicBp} mmHg", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (patientData.systolicBp >= 140) ArterialRedGlow else TextPrimaryDark)
                            }
                            Slider(
                                value = patientData.systolicBp.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(systolicBp = it.roundToInt()) } },
                                valueRange = 90f..210f,
                                colors = SliderDefaults.colors(thumbColor = ArterialRedPrimary, activeTrackColor = ArterialRedPrimary),
                                modifier = Modifier.testTag("slider_systolic_bp")
                            )
                        }

                        // Diastolic BP
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diastolic BP (mmHg)", fontSize = 12.sp, color = TextSecondaryDark)
                                Text("${patientData.diastolicBp} mmHg", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                            Slider(
                                value = patientData.diastolicBp.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(diastolicBp = it.roundToInt()) } },
                                valueRange = 55f..125f,
                                colors = SliderDefaults.colors(thumbColor = ArterialRedPrimary, activeTrackColor = ArterialRedPrimary)
                            )
                        }

                        // Pulse Rate
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Resting Pulse Rate (BPM)", fontSize = 12.sp, color = TextSecondaryDark)
                                Text("${patientData.pulseRate} bpm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VenousCyan)
                            }
                            Slider(
                                value = patientData.pulseRate.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(pulseRate = it.roundToInt()) } },
                                valueRange = 45f..140f,
                                colors = SliderDefaults.colors(thumbColor = VenousCyan, activeTrackColor = VenousCyan),
                                modifier = Modifier.testTag("slider_pulse_rate")
                            )
                        }
                    }
                }
            }

            // Section 3: Laboratory Biomarkers & Cardiac Enzymes
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = PerfusionHealthyEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Biomarkers & Cardiac Enzymes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        // Cardiac Troponin I
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Cardiac Troponin I", fontSize = 12.sp, color = TextSecondaryDark)
                                    Text("Normal < 0.04 ng/mL", fontSize = 10.sp, color = TextMutedDark)
                                }
                                Text(
                                    text = "${patientData.cardiacTroponinI} ng/mL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (patientData.cardiacTroponinI > 0.04f) ArterialRedGlow else PerfusionHealthyEmerald
                                )
                            }
                            Slider(
                                value = patientData.cardiacTroponinI,
                                onValueChange = { viewModel.updatePatient { p -> p.copy(cardiacTroponinI = (it * 100).roundToInt() / 100f) } },
                                valueRange = 0.01f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (patientData.cardiacTroponinI > 0.04f) ArterialRedGlow else PerfusionHealthyEmerald,
                                    activeTrackColor = if (patientData.cardiacTroponinI > 0.04f) ArterialRedGlow else PerfusionHealthyEmerald
                                ),
                                modifier = Modifier.testTag("slider_troponin")
                            )
                        }

                        // LDL-C Cholesterol
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("LDL-C (Atherogenic)", fontSize = 12.sp, color = TextSecondaryDark)
                                Text("${patientData.ldlCholesterol} mg/dL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (patientData.ldlCholesterol >= 160) ArterialRedGlow else TextPrimaryDark)
                            }
                            Slider(
                                value = patientData.ldlCholesterol.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(ldlCholesterol = it.roundToInt()) } },
                                valueRange = 50f..240f,
                                colors = SliderDefaults.colors(thumbColor = ArterialWarningAmber, activeTrackColor = ArterialWarningAmber)
                            )
                        }

                        // Total Cholesterol & Fasting Blood Sugar
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Chol", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${patientData.totalCholesterol}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Slider(
                                    value = patientData.totalCholesterol.toFloat(),
                                    onValueChange = { viewModel.updatePatient { p -> p.copy(totalCholesterol = it.roundToInt()) } },
                                    valueRange = 120f..320f
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("FBS (Sugar)", fontSize = 11.sp, color = TextSecondaryDark)
                                    Text("${patientData.fastingBloodSugar}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Slider(
                                    value = patientData.fastingBloodSugar.toFloat(),
                                    onValueChange = { viewModel.updatePatient { p -> p.copy(fastingBloodSugar = it.roundToInt()) } },
                                    valueRange = 70f..250f
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: ECG Features Matrix (Key input for vessel localization)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("12-Lead ECG Features Matrix", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        // ST Elevation Master Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ST-Segment Elevation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text("Acute transmural ischemia / STEMI pattern", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                            Switch(
                                checked = patientData.stElevation,
                                onCheckedChange = { checked ->
                                    viewModel.updatePatient { p ->
                                        p.copy(
                                            stElevation = checked,
                                            stElevationLeads = if (checked && p.stElevationLeads.isEmpty()) setOf(EcgLead.V2, EcgLead.V3) else p.stElevationLeads
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ArterialRedGlow, checkedTrackColor = ArterialRedPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("switch_st_elevation")
                            )
                        }

                        // Lead Selector (Territory link: V1-V4 LAD, II/III/aVF RCA, I/aVL/V5/V6 LCX)
                        if (patientData.stElevation) {
                            Text("Select Leads with Significant ST Elevation:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EcgLead.values().forEach { lead ->
                                    val isSelected = patientData.stElevationLeads.contains(lead)
                                    val leadTerritoryColor = when (lead.territory) {
                                        "Septal", "Anterior" -> LadVesselColor
                                        "High Lateral", "Apical/Lateral" -> LcxVesselColor
                                        else -> RcaVesselColor
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updatePatient { p ->
                                                val next = if (isSelected) p.stElevationLeads - lead else p.stElevationLeads + lead
                                                p.copy(stElevationLeads = next)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "${lead.leadName} (${lead.territory})",
                                                fontSize = 10.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = leadTerritoryColor.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White,
                                            containerColor = CardioSurfaceVariantDark,
                                            labelColor = TextSecondaryDark
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = CardioBorderDark,
                                            selectedBorderColor = leadTerritoryColor
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = CardioBorderDark, thickness = 0.6.dp)

                        // Other ECG Findings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("T-Wave Inversion (Subendocardial Ischemia)", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(
                                checked = patientData.tWaveInversion,
                                onCheckedChange = { viewModel.updatePatient { p -> p.copy(tWaveInversion = it) } }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Left Ventricular Hypertrophy (LVH)", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(
                                checked = patientData.leftVentricularHypertrophy,
                                onCheckedChange = { viewModel.updatePatient { p -> p.copy(leftVentricularHypertrophy = it) } }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pathological Q-Waves (Prior Infarct)", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(
                                checked = patientData.pathologicalQWaves,
                                onCheckedChange = { viewModel.updatePatient { p -> p.copy(pathologicalQWaves = it) } }
                            )
                        }
                    }
                }
            }

            // Section 5: Echocardiography (LVEF & RWMA Regions)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = VenousCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Echocardiography & RWMA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }

                        // LVEF %
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Left Ventricular Ejection Fraction (LVEF)", fontSize = 12.sp, color = TextSecondaryDark)
                                Text(
                                    text = "${patientData.ejectionFraction}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (patientData.ejectionFraction < 50) ArterialRedGlow else PerfusionHealthyEmerald
                                )
                            }
                            Slider(
                                value = patientData.ejectionFraction.toFloat(),
                                onValueChange = { viewModel.updatePatient { p -> p.copy(ejectionFraction = it.roundToInt()) } },
                                valueRange = 20f..75f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (patientData.ejectionFraction < 50) ArterialRedGlow else PerfusionHealthyEmerald,
                                    activeTrackColor = if (patientData.ejectionFraction < 50) ArterialRedGlow else PerfusionHealthyEmerald
                                ),
                                modifier = Modifier.testTag("slider_lvef")
                            )
                        }

                        // RWMA Region Selection
                        Text(
                            text = "Regional Wall Motion Abnormality (RWMA) Zones:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Echocardiographic abnormal wall motion directly maps onto the corresponding coronary perfusion territory:",
                            fontSize = 10.sp,
                            color = TextMutedDark
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RwmaRegion.values().forEach { region ->
                                val isSelected = patientData.rwmaRegions.contains(region)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updatePatient { p ->
                                            val next = if (region == RwmaRegion.NONE) {
                                                setOf(RwmaRegion.NONE)
                                            } else {
                                                val filtered = p.rwmaRegions.filter { it != RwmaRegion.NONE }.toSet()
                                                if (isSelected) {
                                                    val res = filtered - region
                                                    if (res.isEmpty()) setOf(RwmaRegion.NONE) else res
                                                } else {
                                                    filtered + region
                                                }
                                            }
                                            p.copy(rwmaRegions = next)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (region == RwmaRegion.NONE) "Normal (None)" else "${region.displayName} (${region.linkedVessel.code})",
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (region == RwmaRegion.NONE) PerfusionHealthyEmerald.copy(alpha = 0.2f) else ArterialRedPrimary.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        containerColor = CardioSurfaceVariantDark,
                                        labelColor = TextSecondaryDark
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = CardioBorderDark,
                                        selectedBorderColor = if (region == RwmaRegion.NONE) PerfusionHealthyEmerald else ArterialRedPrimary
                                    ),
                                    modifier = Modifier.testTag("rwma_chip_${region.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Section 6: Clinical Risk Factors
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Traditional Risk Factors", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Typical Anginal Symptoms (Exertional)", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(checked = patientData.typicalAngina, onCheckedChange = { viewModel.updatePatient { p -> p.copy(typicalAngina = it) } })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Diabetes Mellitus", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(checked = patientData.diabetic, onCheckedChange = { viewModel.updatePatient { p -> p.copy(diabetic = it) } })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Hypertension", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(checked = patientData.hypertensive, onCheckedChange = { viewModel.updatePatient { p -> p.copy(hypertensive = it) } })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Tobacco Smoker", fontSize = 12.sp, color = TextPrimaryDark)
                            Switch(checked = patientData.smoker, onCheckedChange = { viewModel.updatePatient { p -> p.copy(smoker = it) } })
                        }
                    }
                }
            }
        }

        // Sticky Bottom Live Prediction HUD Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = CardioSurfaceVariantDark.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (assessment.overallCadPositive) "CAD POSITIVE (≥50%)" else "CAD NEGATIVE (<50%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald
                        )
                    }
                    Text(
                        text = "LAD: ${assessment.vesselPredictions[VesselType.LAD]?.estimatedStenosisPercent}% | LCX: ${assessment.vesselPredictions[VesselType.LCX]?.estimatedStenosisPercent}% | RCA: ${assessment.vesselPredictions[VesselType.RCA]?.estimatedStenosisPercent}%",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                Button(
                    onClick = onViewIn3D,
                    colors = ButtonDefaults.buttonColors(containerColor = ArterialRedPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("apply_and_view_3d")
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View 3D", fontSize = 12.sp)
                }
            }
        }
    }
}
