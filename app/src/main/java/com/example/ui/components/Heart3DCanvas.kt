package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.visualization.*
import kotlin.math.sin

@Composable
fun Heart3DCanvas(
    assessment: CadRiskAssessment,
    pulseRate: Int,
    selectedVessel: VesselType?,
    onSelectVessel: (VesselType?) -> Unit,
    showRwmaOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(25f) }
    var pitch by remember { mutableFloatStateOf(-12f) }
    var scale by remember { mutableFloatStateOf(1.85f) }
    var isAutoRotating by remember { mutableStateOf(false) }

    // Heartbeat pulsation frequency based on patient pulse rate
    val bpm = pulseRate.coerceIn(40, 150)
    val beatDurationMs = (60_000f / bpm).toInt()

    val infiniteTransition = rememberInfiniteTransition(label = "CardiacCycle")
    val heartbeatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(beatDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeatCycle"
    )

    // Auto rotate orbit
    LaunchedEffect(isAutoRotating) {
        if (isAutoRotating) {
            while (true) {
                withFrameNanos {
                    yaw = (yaw + 0.45f) % 360f
                }
            }
        }
    }

    // Pre-calculate heart hull mesh
    val hullPolygons = remember { HeartAnatomy3DData.generateHeartHullMesh() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardioSurfaceDark)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    isAutoRotating = false
                    yaw = (yaw + dragAmount.x * 0.45f) % 360f
                    pitch = (pitch - dragAmount.y * 0.45f).coerceIn(-75f, 75f)
                }
            }
            .testTag("heart_3d_viewport")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + 10f

            // Systolic contraction pulse factor
            val systolicPhase = sin(heartbeatAnim * 2 * Math.PI).toFloat()
            val pulseScale = 1.0f + (systolicPhase * 0.045f)

            // 1. Draw Subtle Radial Perfusion Background Glow
            val glowColor = if (assessment.overallCadPositive) {
                Color(0x33FF1744)
            } else {
                Color(0x2200E676)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = size.width * 0.55f
                )
            )

            // 2. Project Great Vessels (Aorta & Pulmonary Trunk)
            drawVesselTube(
                nodes = HeartAnatomy3DData.AORTA_ARCH,
                pitch = pitch,
                yaw = yaw,
                scale = scale * pulseScale,
                centerX = centerX,
                centerY = centerY,
                baseColor = Color(0xFFE53935),
                glowColor = Color(0x66FF5252),
                strokeWidth = 14f
            )

            drawVesselTube(
                nodes = HeartAnatomy3DData.PULMONARY_TRUNK,
                pitch = pitch,
                yaw = yaw,
                scale = scale * pulseScale,
                centerX = centerX,
                centerY = centerY,
                baseColor = Color(0xFF1E88E5),
                glowColor = Color(0x6600E5FF),
                strokeWidth = 12f
            )

            // 3. Render Depth-Sorted Heart Hull Mesh with RWMA Ischemia Highlight
            val rotatedPolys = hullPolygons.map { poly ->
                val r1 = poly.p1.rotate(pitch, yaw)
                val r2 = poly.p2.rotate(pitch, yaw)
                val r3 = poly.p3.rotate(pitch, yaw)
                val avgZ = (r1.z + r2.z + r3.z) / 3f
                PolyWithDepth(poly, r1, r2, r3, avgZ)
            }.sortedBy { it.avgZ }

            rotatedPolys.forEach { item ->
                val p1Proj = item.r1.project(centerX, centerY, scale * pulseScale)
                val p2Proj = item.r2.project(centerX, centerY, scale * pulseScale)
                val p3Proj = item.r3.project(centerX, centerY, scale * pulseScale)

                val path = Path().apply {
                    moveTo(p1Proj.x, p1Proj.y)
                    lineTo(p2Proj.x, p2Proj.y)
                    lineTo(p3Proj.x, p3Proj.y)
                    close()
                }

                // Check if this facet belongs to an active RWMA abnormality
                val isAbnormalRwma = showRwmaOverlay &&
                        item.poly.region != null &&
                        assessment.vesselPredictions.values.any { pred ->
                            pred.isSignificantStenosis && when (item.poly.region) {
                                RwmaRegion.ANTERIOR, RwmaRegion.SEPTAL, RwmaRegion.APICAL -> pred.vessel == VesselType.LAD
                                RwmaRegion.LATERAL -> pred.vessel == VesselType.LCX
                                RwmaRegion.INFERIOR -> pred.vessel == VesselType.RCA
                                else -> false
                            }
                        }

                val facetColor = when {
                    isAbnormalRwma -> {
                        val pulseAlert = (sin(heartbeatAnim * 2 * Math.PI) * 0.3f + 0.7f).toFloat()
                        Color(0xFFFF1744).copy(alpha = 0.55f * pulseAlert)
                    }
                    showRwmaOverlay && item.poly.region != null && item.poly.region != RwmaRegion.NONE -> {
                        item.poly.baseColor.copy(alpha = 0.45f)
                    }
                    else -> item.poly.baseColor.copy(alpha = 0.35f)
                }

                drawPath(path = path, color = facetColor)

                // Wireframe outline for high-tech anatomical look
                val wireColor = if (isAbnormalRwma) {
                    Color(0xFFFF5252).copy(alpha = 0.6f)
                } else {
                    Color(0xFF334E68).copy(alpha = 0.25f)
                }
                drawPath(path = path, color = wireColor, style = Stroke(width = 1.2f))
            }

            // 4. Render Target Coronary Vessels: LAD, LCX, RCA
            val ladPred = assessment.vesselPredictions[VesselType.LAD]
            val lcxPred = assessment.vesselPredictions[VesselType.LCX]
            val rcaPred = assessment.vesselPredictions[VesselType.RCA]

            // Draw RCA (Right Coronary Artery)
            val rcaColor = getVesselStenosisColor(rcaPred?.estimatedStenosisPercent ?: 15)
            drawCoronaryArteryWithStenosis(
                nodes = HeartAnatomy3DData.RCA_NODES,
                branches = listOf(HeartAnatomy3DData.RCA_ACUTE_MARGINAL),
                pitch = pitch,
                yaw = yaw,
                scale = scale * pulseScale,
                centerX = centerX,
                centerY = centerY,
                vesselColor = rcaColor,
                stenosisPercent = rcaPred?.estimatedStenosisPercent ?: 15,
                isSelected = selectedVessel == VesselType.RCA,
                animPhase = heartbeatAnim
            )

            // Draw LCX (Left Circumflex)
            val lcxColor = getVesselStenosisColor(lcxPred?.estimatedStenosisPercent ?: 15)
            drawCoronaryArteryWithStenosis(
                nodes = HeartAnatomy3DData.LCX_NODES,
                branches = listOf(HeartAnatomy3DData.LCX_OBTUSE_MARGINAL),
                pitch = pitch,
                yaw = yaw,
                scale = scale * pulseScale,
                centerX = centerX,
                centerY = centerY,
                vesselColor = lcxColor,
                stenosisPercent = lcxPred?.estimatedStenosisPercent ?: 15,
                isSelected = selectedVessel == VesselType.LCX,
                animPhase = heartbeatAnim
            )

            // Draw LAD (Left Anterior Descending - Anterior front of the heart)
            val ladColor = getVesselStenosisColor(ladPred?.estimatedStenosisPercent ?: 15)
            drawCoronaryArteryWithStenosis(
                nodes = HeartAnatomy3DData.LAD_NODES,
                branches = listOf(HeartAnatomy3DData.LAD_DIAGONAL_1),
                pitch = pitch,
                yaw = yaw,
                scale = scale * pulseScale,
                centerX = centerX,
                centerY = centerY,
                vesselColor = ladColor,
                stenosisPercent = ladPred?.estimatedStenosisPercent ?: 15,
                isSelected = selectedVessel == VesselType.LAD,
                animPhase = heartbeatAnim
            )
        }

        // Top Floating HUD Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CardioSurfaceVariantDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (assessment.overallCadPositive) "CAD POSITIVE (≥50% Stenosis)" else "CAD NEGATIVE (<50%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (assessment.overallCadPositive) ArterialRedGlow else PerfusionHealthyEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = CardioSurfaceVariantDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardioBorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pulseRate} BPM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VenousCyan
                    )
                }
            }
        }

        // Right Action Controls (Reset, Orbit, Zoom, RWMA)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { isAutoRotating = !isAutoRotating },
                modifier = Modifier
                    .size(38.dp)
                    .background(CardioSurfaceVariantDark.copy(alpha = 0.85f), CircleShape)
                    .testTag("toggle_orbit_button")
            ) {
                Icon(
                    imageVector = if (isAutoRotating) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Orbit",
                    tint = if (isAutoRotating) VenousCyan else TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = {
                    yaw = 25f
                    pitch = -12f
                    scale = 1.85f
                    isAutoRotating = false
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(CardioSurfaceVariantDark.copy(alpha = 0.85f), CircleShape)
                    .testTag("reset_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset 3D View",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Vessel Quick Selector Chips
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VesselType.values().forEach { vessel ->
                val pred = assessment.vesselPredictions[vessel]
                val percent = pred?.estimatedStenosisPercent ?: 0
                val isSelected = selectedVessel == vessel
                val isSevere = percent >= 50

                Surface(
                    onClick = {
                        onSelectVessel(if (isSelected) null else vessel)
                    },
                    color = if (isSelected) CardioSurfaceHighlight else CardioSurfaceVariantDark.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) VenousCyan else if (isSevere) ArterialRedGlow.copy(alpha = 0.6f) else CardioBorderDark
                    ),
                    modifier = Modifier.testTag("vessel_chip_${vessel.code.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(getVesselStenosisColor(percent), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${vessel.code}: $percent%",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSevere) ArterialRedGlow else TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}

private data class PolyWithDepth(
    val poly: HeartMeshPolygon,
    val r1: Point3D,
    val r2: Point3D,
    val r3: Point3D,
    val avgZ: Float
)

private fun getVesselStenosisColor(stenosisPercent: Int): Color {
    return when {
        stenosisPercent >= 70 -> Color(0xFFFF1744) // Severe Critical Ruby Red
        stenosisPercent >= 50 -> Color(0xFFFF5252) // Significant Stenosis Red
        stenosisPercent >= 30 -> Color(0xFFFFB300) // Moderate Warning Amber
        else -> Color(0xFF00E676)                  // Healthy Emerald
    }
}

private fun DrawScope.drawVesselTube(
    nodes: List<Point3D>,
    pitch: Float,
    yaw: Float,
    scale: Float,
    centerX: Float,
    centerY: Float,
    baseColor: Color,
    glowColor: Color,
    strokeWidth: Float
) {
    if (nodes.size < 2) return
    val projected = nodes.map { it.rotate(pitch, yaw).project(centerX, centerY, scale) }

    val path = Path().apply {
        moveTo(projected.first().x, projected.first().y)
        for (i in 1 until projected.size) {
            val curr = projected[i]
            lineTo(curr.x, curr.y)
        }
    }

    // Outer Glow
    drawPath(path = path, color = glowColor, style = Stroke(width = strokeWidth * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Core Vessel
    drawPath(path = path, color = baseColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawCoronaryArteryWithStenosis(
    nodes: List<Point3D>,
    branches: List<List<Point3D>>,
    pitch: Float,
    yaw: Float,
    scale: Float,
    centerX: Float,
    centerY: Float,
    vesselColor: Color,
    stenosisPercent: Int,
    isSelected: Boolean,
    animPhase: Float
) {
    val rotatedNodes = nodes.map { it.rotate(pitch, yaw) }
    val projectedNodes = rotatedNodes.map { it.project(centerX, centerY, scale) }

    val isSignificant = stenosisPercent >= 50
    val baseWidth = if (isSelected) 8.5f else 6f

    // 1. Draw Artery Segments
    for (i in 0 until projectedNodes.size - 1) {
        val p1 = projectedNodes[i]
        val p2 = projectedNodes[i + 1]

        // If mid vessel (around index 2-3), apply narrowing if stenosed
        val isStenosisSite = (i == 2 || i == 3) && isSignificant
        val segWidth = if (isStenosisSite) (baseWidth * (1f - (stenosisPercent / 100f) * 0.65f)).coerceAtLeast(2.0f) else baseWidth

        // Outer Neon Glow
        drawLine(
            color = vesselColor.copy(alpha = if (isSelected) 0.5f else 0.3f),
            start = Offset(p1.x, p1.y),
            end = Offset(p2.x, p2.y),
            strokeWidth = segWidth * 2.2f,
            cap = StrokeCap.Round
        )

        // Core Lumen
        drawLine(
            color = vesselColor,
            start = Offset(p1.x, p1.y),
            end = Offset(p2.x, p2.y),
            strokeWidth = segWidth,
            cap = StrokeCap.Round
        )
    }

    // 2. Draw Anatomical Branches (Diagonals / Marginals)
    branches.forEach { branch ->
        val branchProj = branch.map { it.rotate(pitch, yaw).project(centerX, centerY, scale) }
        for (i in 0 until branchProj.size - 1) {
            val bp1 = branchProj[i]
            val bp2 = branchProj[i + 1]
            drawLine(
                color = vesselColor.copy(alpha = 0.8f),
                start = Offset(bp1.x, bp1.y),
                end = Offset(bp2.x, bp2.y),
                strokeWidth = baseWidth * 0.65f,
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Stenosis Lesion Plaque Marker (if >= 50%)
    if (isSignificant && projectedNodes.size > 3) {
        val lesionPoint = projectedNodes[2]
        // Pulsating stenosis plaque indicator
        val pulseRing = 12f + sin(animPhase * 2 * Math.PI).toFloat() * 3.5f

        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = 0.35f),
            radius = pulseRing,
            center = Offset(lesionPoint.x, lesionPoint.y)
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 5.5f,
            center = Offset(lesionPoint.x, lesionPoint.y)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset(lesionPoint.x, lesionPoint.y)
        )
    }

    // 4. Blood Flow Particles along vessel
    val particleCount = 4
    for (k in 0 until particleCount) {
        // Particles advance with speed inversely proportional to stenosis
        val speedFactor = if (isSignificant) 0.5f else 1.0f
        val offsetFactor = (animPhase * speedFactor + k.toFloat() / particleCount) % 1.0f

        val totalSegments = projectedNodes.size - 1
        val segFloat = offsetFactor * totalSegments
        val segIdx = segFloat.toInt().coerceIn(0, totalSegments - 1)
        val frac = segFloat - segIdx

        val start = projectedNodes[segIdx]
        val end = projectedNodes[segIdx + 1]

        val partX = start.x + (end.x - start.x) * frac
        val partY = start.y + (end.y - start.y) * frac

        val partColor = if (isSignificant && segIdx >= 2) Color(0xFFFF8A80) else Color(0xFFE0F7FA)
        drawCircle(
            color = partColor,
            radius = if (isSelected) 3f else 2.2f,
            center = Offset(partX, partY)
        )
    }

    // 5. Selected Vessel Indicator Target Halo
    if (isSelected && projectedNodes.isNotEmpty()) {
        val root = projectedNodes.first()
        drawCircle(
            color = VenousCyan.copy(alpha = 0.45f),
            radius = 16f,
            center = Offset(root.x, root.y),
            style = Stroke(width = 2f)
        )
    }
}
