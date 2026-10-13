package com.example.visualization

import androidx.compose.ui.graphics.Color
import com.example.model.RwmaRegion
import com.example.model.VesselSeverity
import com.example.model.VesselType
import kotlin.math.*

data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun rotate(pitchDeg: Float, yawDeg: Float, rollDeg: Float = 0f): Point3D {
        val pitch = Math.toRadians(pitchDeg.toDouble())
        val yaw = Math.toRadians(yawDeg.toDouble())
        val roll = Math.toRadians(rollDeg.toDouble())

        // Yaw (Y axis)
        val cosY = cos(yaw).toFloat()
        val sinY = sin(yaw).toFloat()
        val x1 = x * cosY + z * sinY
        val y1 = y
        val z1 = -x * sinY + z * cosY

        // Pitch (X axis)
        val cosP = cos(pitch).toFloat()
        val sinP = sin(pitch).toFloat()
        val x2 = x1
        val y2 = y1 * cosP - z1 * sinP
        val z2 = y1 * sinP + z1 * cosP

        // Roll (Z axis)
        val cosR = cos(roll).toFloat()
        val sinR = sin(roll).toFloat()
        val x3 = x2 * cosR - y2 * sinR
        val y3 = x2 * sinR + y2 * cosR
        val z3 = z2

        return Point3D(x3, y3, z3)
    }

    fun project(centerX: Float, centerY: Float, scale: Float, fovDistance: Float = 360f): Point2DProjected {
        val depth = (z + fovDistance).coerceAtLeast(40f)
        val factor = (fovDistance / depth) * scale
        val projX = centerX + x * factor
        val projY = centerY + y * factor
        return Point2DProjected(projX, projY, z, factor)
    }
}

data class Point2DProjected(
    val x: Float,
    val y: Float,
    val depthZ: Float,
    val factor: Float
)

data class VesselSegment3D(
    val vesselType: VesselType,
    val segmentIndex: Int,
    val start: Point3D,
    val end: Point3D,
    val normalWidth: Float = 6f
)

data class HeartMeshPolygon(
    val p1: Point3D,
    val p2: Point3D,
    val p3: Point3D,
    val baseColor: Color,
    val region: RwmaRegion? = null
)

object HeartAnatomy3DData {

    // 3D paths for LAD, LCX, RCA
    val LAD_NODES = listOf(
        Point3D(-12f, -32f, 28f), // Ostium / LMCA bifurcation
        Point3D(-14f, -18f, 36f), // Proximal LAD
        Point3D(-12f, 0f, 42f),   // Mid LAD (First diagonal branch D1 takeoff)
        Point3D(-8f, 22f, 44f),   // Mid-distal LAD
        Point3D(-4f, 46f, 38f),   // Distal LAD
        Point3D(0f, 68f, 24f),    // Apical wrap
        Point3D(3f, 82f, 8f)      // Apex
    )

    val LAD_DIAGONAL_1 = listOf(
        Point3D(-12f, 0f, 42f),
        Point3D(-24f, 12f, 36f),
        Point3D(-34f, 28f, 26f)
    )

    val LCX_NODES = listOf(
        Point3D(-12f, -32f, 28f), // LMCA
        Point3D(-26f, -30f, 24f), // Proximal LCX in AV groove
        Point3D(-42f, -22f, 12f), // Encircling obtuse margin
        Point3D(-50f, -6f, -6f),  // Mid LCX (OM1 branch takeoff)
        Point3D(-46f, 16f, -22f), // Distal LCX posterior
        Point3D(-32f, 36f, -30f)  // Posterolateral branch
    )

    val LCX_OBTUSE_MARGINAL = listOf(
        Point3D(-50f, -6f, -6f),
        Point3D(-46f, 14f, 10f),
        Point3D(-38f, 32f, 18f)
    )

    val RCA_NODES = listOf(
        Point3D(14f, -30f, 26f),  // RCA Ostium (Right coronary sinus)
        Point3D(30f, -24f, 26f),  // Proximal RCA in right AV groove
        Point3D(42f, -6f, 20f),   // Mid RCA (Right acute margin)
        Point3D(42f, 16f, 8f),    // Distal RCA approaching crux
        Point3D(34f, 38f, -6f),   // Crux of the heart
        Point3D(18f, 54f, -18f),  // Posterior Descending Artery (PDA)
        Point3D(6f, 72f, -8f)     // Terminal PDA
    )

    val RCA_ACUTE_MARGINAL = listOf(
        Point3D(42f, -6f, 20f),
        Point3D(46f, 12f, 28f),
        Point3D(38f, 34f, 32f)
    )

    // Structural Anatomical Heart Hull Points (Lat/Long sphere-like deformation forming cardiac shape)
    fun generateHeartHullMesh(): List<HeartMeshPolygon> {
        val polygons = mutableListOf<HeartMeshPolygon>()
        val slices = 9
        val stacks = 9

        val grid = Array(slices + 1) { Array(stacks + 1) { Point3D(0f, 0f, 0f) } }

        for (i in 0..slices) {
            val u = i.toFloat() / slices // 0 to 1 (top to bottom)
            val y = -40f + u * 120f      // from -40 (base/atria) to 80 (apex)

            // Radius profile: heart shape (wide at base/mid-ventricle, tapered at apex)
            val radius = when {
                u < 0.2f -> 38f + u * 35f
                u < 0.6f -> 52f - (u - 0.2f) * 15f
                else -> 46f * (1f - (u - 0.6f) / 0.4f).coerceAtLeast(0.08f)
            }

            for (j in 0..stacks) {
                val v = j.toFloat() / stacks // 0 to 1 (around heart)
                val angle = (v * 2 * Math.PI).toFloat()

                // Asymmetric cardiac deformation (LV is larger, anterior-lateral bulge)
                val xDistort = if (cos(angle) < 0) 1.15f else 0.95f
                val zDistort = if (sin(angle) > 0) 1.1f else 0.85f

                val x = cos(angle) * radius * xDistort
                val z = sin(angle) * radius * zDistort

                grid[i][j] = Point3D(x, y, z)
            }
        }

        // Build Triangles
        for (i in 0 until slices) {
            val u = i.toFloat() / slices
            for (j in 0 until stacks) {
                val p00 = grid[i][j]
                val p01 = grid[i][j + 1]
                val p10 = grid[i + 1][j]
                val p11 = grid[i + 1][j + 1]

                // Determine anatomical region for Echo RWMA mapping
                val avgZ = (p00.z + p01.z + p10.z + p11.z) / 4f
                val avgX = (p00.x + p01.x + p10.x + p11.x) / 4f
                val avgY = (p00.y + p01.y + p10.y + p11.y) / 4f

                val region = when {
                    avgY > 50f -> RwmaRegion.APICAL
                    avgZ > 15f && avgX <= 5f -> RwmaRegion.ANTERIOR
                    avgZ > 15f && avgX > 5f -> RwmaRegion.SEPTAL
                    avgX < -15f -> RwmaRegion.LATERAL
                    avgZ < -10f -> RwmaRegion.INFERIOR
                    else -> RwmaRegion.NONE
                }

                val baseTone = when (region) {
                    RwmaRegion.ANTERIOR -> Color(0xFF23304E)
                    RwmaRegion.SEPTAL -> Color(0xFF1E2A45)
                    RwmaRegion.LATERAL -> Color(0xFF28244B)
                    RwmaRegion.INFERIOR -> Color(0xFF33253B)
                    RwmaRegion.APICAL -> Color(0xFF253344)
                    else -> Color(0xFF1B2337)
                }

                polygons.add(HeartMeshPolygon(p00, p10, p11, baseTone, region))
                polygons.add(HeartMeshPolygon(p00, p11, p01, baseTone, region))
            }
        }

        return polygons
    }

    // Great Vessels (Aorta and Pulmonary Artery)
    val AORTA_ARCH = listOf(
        Point3D(0f, -35f, 15f),
        Point3D(2f, -55f, 12f),
        Point3D(-4f, -72f, 0f),
        Point3D(-16f, -76f, -14f),
        Point3D(-24f, -65f, -26f)
    )

    val PULMONARY_TRUNK = listOf(
        Point3D(-10f, -32f, 25f),
        Point3D(-14f, -50f, 22f),
        Point3D(-22f, -60f, 12f)
    )
}
