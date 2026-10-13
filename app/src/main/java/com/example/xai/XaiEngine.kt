package com.example.xai

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class FeatureAttribution(
    val featureName: String,
    val category: String, // "Biomarker", "ECG", "Vitals", "History", "Echo"
    val attributionPoints: Float, // Positive increases risk, negative decreases risk
    val percentageOfRisk: Int, // Relative share
    val currentValueDisplay: String,
    val referenceNormDisplay: String,
    val clinicalMechanism: String,
    val isPrimaryDriver: Boolean = false
)

data class EcgTelemetryData(
    val rhythm: String,
    val heartRateBpm: Int,
    val prIntervalMs: Int,
    val qrsDurationMs: Int,
    val qtcIntervalMs: Int,
    val stElevationMap: Map<String, Double>, // lead to mV
    val tWaveInversionLeads: List<String>,
    val pathologicalQWaveLeads: List<String>
)

object XaiEngine {

    fun computeFeatureAttributions(
        patient: PatientClinicalData,
        assessment: CadRiskAssessment
    ): List<FeatureAttribution> {
        val list = mutableListOf<FeatureAttribution>()

        // 1. Cardiac Enzymes (Troponin I)
        if (patient.cardiacTroponinI > 0.04f) {
            val pts = if (patient.cardiacTroponinI >= 1.0f) 26f else if (patient.cardiacTroponinI >= 0.2f) 20f else 14f
            list.add(
                FeatureAttribution(
                    featureName = "High-Sensitivity Troponin I",
                    category = "Biomarker",
                    attributionPoints = pts,
                    percentageOfRisk = 0,
                    currentValueDisplay = "${patient.cardiacTroponinI} ng/mL",
                    referenceNormDisplay = "< 0.04 ng/mL",
                    clinicalMechanism = "Indicates active cardiomyocyte necrosis from acute epicardial coronary flow limitation or microvascular injury."
                )
            )
        }

        // 2. ECG ST-Segment Elevation
        if (patient.stElevation) {
            val leadCount = patient.stElevationLeads.size
            val pts = 16f + (leadCount * 3.5f)
            val leadsStr = patient.stElevationLeads.joinToString { it.leadName }
            list.add(
                FeatureAttribution(
                    featureName = "ST-Segment Elevation ($leadsStr)",
                    category = "ECG",
                    attributionPoints = pts,
                    percentageOfRisk = 0,
                    currentValueDisplay = "Present in $leadCount leads",
                    referenceNormDisplay = "Isoelectric baseline (< 0.1 mV)",
                    clinicalMechanism = "Direct hallmark of acute transmural epicardial ischemia in the corresponding vessel distribution."
                )
            )
        }

        // 3. Systolic Blood Pressure
        if (patient.systolicBp >= 130) {
            val delta = patient.systolicBp - 120
            val pts = (delta * 0.28f).coerceIn(4f, 22f)
            list.add(
                FeatureAttribution(
                    featureName = "Systolic Blood Pressure",
                    category = "Vitals",
                    attributionPoints = pts,
                    percentageOfRisk = 0,
                    currentValueDisplay = "${patient.systolicBp} mmHg",
                    referenceNormDisplay = "< 120 mmHg",
                    clinicalMechanism = "Elevated hydraulic shear stress accelerates vascular endothelial damage and atheroma cap thinning."
                )
            )
        }

        // 4. Atherogenic LDL-C
        if (patient.ldlCholesterol >= 100) {
            val delta = patient.ldlCholesterol - 100
            val pts = (delta * 0.18f).coerceIn(3f, 20f)
            list.add(
                FeatureAttribution(
                    featureName = "Atherogenic LDL-C",
                    category = "Biomarker",
                    attributionPoints = pts,
                    percentageOfRisk = 0,
                    currentValueDisplay = "${patient.ldlCholesterol} mg/dL",
                    referenceNormDisplay = "< 70 mg/dL (AHA optimal)",
                    clinicalMechanism = "Apolipoprotein B particle retention in the arterial intima fuels macrophage foam cell formation and necrotic core expansion."
                )
            )
        }

        // 5. Active Smoking
        if (patient.smoker) {
            list.add(
                FeatureAttribution(
                    featureName = "Tobacco Inhalation / Smoking",
                    category = "History",
                    attributionPoints = 12f,
                    percentageOfRisk = 0,
                    currentValueDisplay = "Active Smoker",
                    referenceNormDisplay = "Non-Smoker",
                    clinicalMechanism = "Promotes platelet aggregation, nitric oxide depletion, acute coronary vasospasm, and elevated fibrinogen."
                )
            )
        }

        // 6. Diabetes Mellitus
        if (patient.diabetic) {
            list.add(
                FeatureAttribution(
                    featureName = "Diabetes Mellitus / Hyperglycemia",
                    category = "History",
                    attributionPoints = 11f,
                    percentageOfRisk = 0,
                    currentValueDisplay = "FBS ${patient.fastingBloodSugar} mg/dL",
                    referenceNormDisplay = "< 100 mg/dL",
                    clinicalMechanism = "Advanced glycation end-products (AGEs) cross-link arterial collagen, provoking diffuse multi-vessel CAD."
                )
            )
        }

        // 7. Impaired LVEF / Wall Motion (Echo)
        if (patient.ejectionFraction < 55) {
            val deficit = 55 - patient.ejectionFraction
            val pts = (deficit * 0.45f).coerceIn(4f, 18f)
            list.add(
                FeatureAttribution(
                    featureName = "Impaired LVEF (${patient.ejectionFraction}%)",
                    category = "Echo",
                    attributionPoints = pts,
                    percentageOfRisk = 0,
                    currentValueDisplay = "${patient.ejectionFraction}%",
                    referenceNormDisplay = "≥ 55% normal",
                    clinicalMechanism = "Decreased systolic contractility reflects extensive prior or ongoing myocardial injury."
                )
            )
        }

        // 8. Regional Wall Motion Abnormality (RWMA)
        if (patient.rwmaRegions.any { it != RwmaRegion.NONE }) {
            val regions = patient.rwmaRegions.filter { it != RwmaRegion.NONE }.joinToString { it.displayName }
            list.add(
                FeatureAttribution(
                    featureName = "Echo RWMA ($regions)",
                    category = "Echo",
                    attributionPoints = 14f,
                    percentageOfRisk = 0,
                    currentValueDisplay = regions,
                    referenceNormDisplay = "Normal wall motion",
                    clinicalMechanism = "Focal akinesis/hypokinesis anatomically localizes severe upstream coronary vessel flow obstruction."
                )
            )
        }

        // 9. Protective factor: High HDL-C
        if (patient.hdlCholesterol >= 50) {
            val bonus = ((patient.hdlCholesterol - 50) * 0.25f).coerceIn(2f, 10f)
            list.add(
                FeatureAttribution(
                    featureName = "Protective HDL-C (Reverse Transport)",
                    category = "Biomarker",
                    attributionPoints = -bonus,
                    percentageOfRisk = 0,
                    currentValueDisplay = "${patient.hdlCholesterol} mg/dL",
                    referenceNormDisplay = "> 40 (M), > 50 (F)",
                    clinicalMechanism = "Promotes reverse cholesterol transport from peripheral atheromas back to the liver for biliary excretion."
                )
            )
        }

        // Sort descending by absolute attribution points
        val sorted = list.sortedByDescending { it.attributionPoints }
        val totalPositive = sorted.filter { it.attributionPoints > 0 }.sumOf { it.attributionPoints.toDouble() }.toFloat().coerceAtLeast(1f)

        return sorted.mapIndexed { idx, item ->
            val relPercent = if (item.attributionPoints > 0) {
                ((item.attributionPoints / totalPositive) * 100).roundToInt()
            } else {
                -((kotlin.math.abs(item.attributionPoints) / totalPositive) * 100).roundToInt()
            }
            item.copy(
                percentageOfRisk = relPercent,
                isPrimaryDriver = idx == 0 && item.attributionPoints > 0
            )
        }
    }

    /**
     * Parses ECG JSON data structure as specified in the hackathon challenge.
     */
    fun parseEcgJson(jsonString: String): Result<EcgTelemetryData> {
        return try {
            val obj = JSONObject(jsonString)
            val rhythm = obj.optString("rhythm", "Normal Sinus Rhythm")
            val hr = obj.optInt("heartRateBpm", 75)
            val pr = obj.optInt("prIntervalMs", 160)
            val qrs = obj.optInt("qrsDurationMs", 88)
            val qtc = obj.optInt("qtcIntervalMs", 420)

            val stMap = mutableMapOf<String, Double>()
            val stObj = obj.optJSONObject("stElevationMv")
            if (stObj != null) {
                val keys = stObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    stMap[k] = stObj.optDouble(k, 0.0)
                }
            }

            val tInversionList = mutableListOf<String>()
            val tArr = obj.optJSONArray("tWaveInversionLeads")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    tInversionList.add(tArr.getString(i))
                }
            }

            val qWaveList = mutableListOf<String>()
            val qArr = obj.optJSONArray("pathologicalQWaveLeads")
            if (qArr != null) {
                for (i in 0 until qArr.length()) {
                    qWaveList.add(qArr.getString(i))
                }
            }

            Result.success(
                EcgTelemetryData(
                    rhythm = rhythm,
                    heartRateBpm = hr,
                    prIntervalMs = pr,
                    qrsDurationMs = qrs,
                    qtcIntervalMs = qtc,
                    stElevationMap = stMap,
                    tWaveInversionLeads = tInversionList,
                    pathologicalQWaveLeads = qWaveList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateSampleEcgJson(patient: PatientClinicalData): String {
        val root = JSONObject()
        root.put("rhythm", if (patient.pulseRate < 60) "Sinus Bradycardia" else if (patient.pulseRate > 100) "Sinus Tachycardia" else "Normal Sinus Rhythm")
        root.put("heartRateBpm", patient.pulseRate)
        root.put("prIntervalMs", 165)
        root.put("qrsDurationMs", if (patient.leftVentricularHypertrophy) 112 else 86)
        root.put("qtcIntervalMs", 432)

        val stObj = JSONObject()
        EcgLead.values().forEach { lead ->
            val hasSt = patient.stElevation && patient.stElevationLeads.contains(lead)
            val amplitude = if (hasSt) 0.28 else 0.02
            stObj.put(lead.leadName, amplitude)
        }
        root.put("stElevationMv", stObj)

        val tArr = JSONArray()
        if (patient.tWaveInversion) {
            tArr.put("V5")
            tArr.put("V6")
            tArr.put("I")
            tArr.put("aVL")
        }
        root.put("tWaveInversionLeads", tArr)

        val qArr = JSONArray()
        if (patient.pathologicalQWaves) {
            qArr.put("V2")
            qArr.put("V3")
        }
        root.put("pathologicalQWaveLeads", qArr)

        return root.toString(2)
    }
}
