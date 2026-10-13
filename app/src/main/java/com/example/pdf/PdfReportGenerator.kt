package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.CadRiskAssessment
import com.example.model.PatientClinicalData
import com.example.model.VesselType
import com.example.xai.XaiEngine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        patient: PatientClinicalData,
        assessment: CadRiskAssessment
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

        // Background
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Top Medical Header Bar
        paint.color = Color.parseColor("#0F172A") // Navy Slate
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CARDIOVISION 3D • CLINICAL ASSESSMENT REPORT", 32f, 42f, paint)

        paint.textSize = 10f
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Multimodal Cardiovascular Risk Visualization & Coronary Stenosis Mapping", 32f, 60f, paint)

        // Patient Metadata Banner
        var y = 102f
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PATIENT: ${patient.name.uppercase()}", 32f, y, paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#475569")
        canvas.drawText("Age: ${patient.age}yo  |  Sex: ${if (patient.isMale) "Male" else "Female"}  |  BMI: ${patient.bmi}  |  Generated: ${dateFormat.format(Date())}", 32f, y + 16f, paint)

        // Overall CAD Status Box
        y += 38f
        val isPositive = assessment.overallCadPositive
        paint.color = if (isPositive) Color.parseColor("#FEE2E2") else Color.parseColor("#DCFCE7")
        canvas.drawRoundRect(32f, y, (pageWidth - 32).toFloat(), y + 54f, 12f, 12f, paint)

        paint.color = if (isPositive) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(32f, y, (pageWidth - 32).toFloat(), y + 54f, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isPositive) Color.parseColor("#991B1B") else Color.parseColor("#166534")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val statusText = if (isPositive) "CAD POSITIVE (≥50% Stenosis Detected)" else "CAD NEGATIVE (<50% Stenosis Across Target Vessels)"
        canvas.drawText(statusText, 48f, y + 25f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Overall Cardiac Risk Score: ${assessment.overallRiskPercent}% (${assessment.riskCategory.label})   •   Framingham 10-Yr: ${assessment.framingham10YrScore}%", 48f, y + 42f, paint)

        // Section: Three Target Vessels Stenosis Matrix
        y += 74f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TARGET CORONARY VESSELS (LAD, LCX, RCA) STENOSIS MATRIX", 32f, y, paint)

        // Table Header
        y += 14f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(32f, y, (pageWidth - 32).toFloat(), y + 22f, paint)
        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("VESSEL", 42f, y + 14f, paint)
        canvas.drawText("ANATOMICAL REGION", 120f, y + 14f, paint)
        canvas.drawText("STENOSIS %", 330f, y + 14f, paint)
        canvas.drawText("PROBABILITY", 410f, y + 14f, paint)
        canvas.drawText("SEVERITY", 485f, y + 14f, paint)

        // Table Rows
        val vessels = listOf(VesselType.LAD, VesselType.LCX, VesselType.RCA)
        vessels.forEachIndexed { i, v ->
            y += 24f
            val pred = assessment.vesselPredictions[v]
            paint.color = if (i % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(32f, y - 16f, (pageWidth - 32).toFloat(), y + 8f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${v.code} (${v.fullName})", 42f, y, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#475569")
            val territoryShort = when (v) {
                VesselType.LAD -> "Anterior wall, septum, apex"
                VesselType.LCX -> "Lateral / posterior LV wall"
                VesselType.RCA -> "Inferior wall, RV, AV node"
            }
            canvas.drawText(territoryShort, 120f, y, paint)

            paint.color = if ((pred?.estimatedStenosisPercent ?: 0) >= 50) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${pred?.estimatedStenosisPercent ?: 0}%", 330f, y, paint)

            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("${((pred?.stenosisProbability ?: 0f) * 100).toInt()}%", 410f, y, paint)

            canvas.drawText(pred?.severity?.label?.split(" ")?.first() ?: "N/A", 485f, y, paint)
        }

        // Section: Biomarker & ECG Clinical Baseline
        y += 28f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KEY CLINICAL & ELECTROCARDIOGRAPHIC MARKERS", 32f, y, paint)

        y += 16f
        paint.color = Color.WHITE
        canvas.drawRoundRect(32f, y, (pageWidth - 32).toFloat(), y + 64f, 8f, 8f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(32f, y, (pageWidth - 32).toFloat(), y + 64f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 9.5f
        paint.color = Color.parseColor("#334155")
        val col1 = "BP: ${patient.systolicBp}/${patient.diastolicBp} mmHg  |  Pulse: ${patient.pulseRate} bpm  |  Troponin I: ${patient.cardiacTroponinI} ng/mL (ref <0.04)"
        val col2 = "Lipids: Total Chol ${patient.totalCholesterol}  |  LDL-C ${patient.ldlCholesterol}  |  HDL-C ${patient.hdlCholesterol}  |  Triglycerides ${patient.triglycerides} mg/dL"
        val col3 = "ECG: ST Elevation: ${patient.stElevation} (Leads: ${patient.stElevationLeads.joinToString { it.leadName }})  |  T-Wave Inversion: ${patient.tWaveInversion}  |  LVH: ${patient.leftVentricularHypertrophy}"
        val col4 = "Echo: LVEF ${patient.ejectionFraction}%  |  RWMA Zones: ${patient.rwmaRegions.joinToString { it.displayName }}"

        canvas.drawText(col1, 44f, y + 16f, paint)
        canvas.drawText(col2, 44f, y + 30f, paint)
        canvas.drawText(col3, 44f, y + 44f, paint)
        canvas.drawText(col4, 44f, y + 58f, paint)

        // Section: XAI Explainable AI Risk Attribution
        y += 82f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EXPLAINABLE AI (XAI) TOP RISK DRIVERS", 32f, y, paint)

        val attributions = XaiEngine.computeFeatureAttributions(patient, assessment).take(4)
        attributions.forEach { attr ->
            y += 18f
            paint.color = Color.parseColor("#DC2626")
            canvas.drawCircle(42f, y - 3f, 3.5f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${attr.featureName} (+${attr.percentageOfRisk}% risk contribution)", 52f, y, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Value: ${attr.currentValueDisplay}  •  ${attr.clinicalMechanism}", 52f, y + 12f, paint)
            y += 12f
        }

        // Section: Guideline-Directed Recommendations
        y += 24f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GUIDELINE-DIRECTED CLINICAL RECOMMENDATIONS", 32f, y, paint)

        assessment.recommendedNextSteps.forEach { step ->
            y += 16f
            paint.color = Color.parseColor("#2563EB")
            canvas.drawCircle(42f, y - 3f, 3f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(step, 52f, y, paint)
        }

        // Footer Disclaimer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        canvas.drawText("Confidential Medical Assessment • CardioVision 3D AI Clinical Decision Support • For Professional Healthcare Use Only", 32f, pageHeight - 24f, paint)

        pdfDocument.finishPage(page)

        // Save to file
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val file = File(reportsDir, "CardioVision_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Launch share sheet
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "CardioVision 3D Medical Summary - ${patient.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Clinical PDF Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
