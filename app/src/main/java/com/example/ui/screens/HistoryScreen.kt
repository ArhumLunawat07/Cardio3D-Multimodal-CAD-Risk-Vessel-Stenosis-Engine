package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AssessmentRecordEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CardioViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: CardioViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.savedRecords.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardioDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clinical Assessment Records",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Persisted patient evaluations & vessel stenosis logs",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    color = CardioSurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
                ) {
                    Text(
                        text = "${records.size} Saved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VenousCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Saved Clinical Records Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Assess patient parameters on the 3D Visualizer tab and tap the Bookmark icon to archive cases here.",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardioSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (record.overallCadPositive) ArterialRedGlow.copy(alpha = 0.5f) else CardioBorderDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.patientName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${dateFormat.format(Date(record.timestamp))} • ${record.age}yo ${if (record.isMale) "M" else "F"}",
                                    fontSize = 11.sp,
                                    color = TextMutedDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (record.overallCadPositive) ArterialRedGlow.copy(alpha = 0.15f) else PerfusionHealthyEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (record.overallCadPositive) "CAD POSITIVE (${record.overallRiskPercent}%)" else "CAD NEGATIVE (${record.overallRiskPercent}%)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { viewModel.deleteRecord(record) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Vessel stenosis badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VesselBadge(label = "LAD", percent = record.ladStenosisPercent)
                            VesselBadge(label = "LCX", percent = record.lcxStenosisPercent)
                            VesselBadge(label = "RCA", percent = record.rcaStenosisPercent)
                        }

                        Text(
                            text = "Troponin I: ${record.troponinI} ng/mL | LVEF: ${record.ejectionFraction}%",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )

                        Text(
                            text = record.clinicalSummary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VesselBadge(label: String, percent: Int) {
    val isSevere = percent >= 50
    Surface(
        color = CardioSurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSevere) ArterialRedGlow.copy(alpha = 0.5f) else CardioBorderDark
        )
    ) {
        Text(
            text = "$label: $percent%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSevere) ArterialRedGlow else PerfusionHealthyEmerald,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
