package com.example.ml

import com.example.model.*
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object CardioRiskEngine {

    val SAMPLE_PRESETS: List<ClinicalCasePreset> = listOf(
        ClinicalCasePreset(
            id = "case_lad_stemi",
            title = "Case 1: Anterior STEMI (High LAD Stenosis)",
            subtitle = "62M, Chest pain, Anterior ST Elevation, RWMA Anterior/Apical",
            badge = "Severe LAD CAD",
            data = PatientClinicalData(
                name = "Case 1: Anterior STEMI (LAD)",
                age = 62,
                isMale = true,
                bmi = 28.6f,
                systolicBp = 152,
                diastolicBp = 94,
                pulseRate = 88,
                totalCholesterol = 245,
                ldlCholesterol = 168,
                hdlCholesterol = 35,
                triglycerides = 210,
                fastingBloodSugar = 138,
                cardiacTroponinI = 1.45f,
                ckMb = 28.5f,
                stElevation = true,
                stElevationLeads = setOf(EcgLead.V1, EcgLead.V2, EcgLead.V3, EcgLead.V4),
                tWaveInversion = false,
                leftVentricularHypertrophy = true,
                pathologicalQWaves = true,
                ejectionFraction = 40,
                rwmaRegions = setOf(RwmaRegion.ANTERIOR, RwmaRegion.SEPTAL, RwmaRegion.APICAL),
                smoker = true,
                diabetic = true,
                hypertensive = true,
                typicalAngina = true
            )
        ),
        ClinicalCasePreset(
            id = "case_multivessel",
            title = "Case 2: Multi-Vessel CAD (LAD + RCA)",
            subtitle = "68M, Diaphoretic, Inferior & Anterior involvement, RWMA Inferior",
            badge = "Multi-Vessel CAD",
            data = PatientClinicalData(
                name = "Case 2: Multi-Vessel CAD (LAD & RCA)",
                age = 68,
                isMale = true,
                bmi = 31.2f,
                systolicBp = 168,
                diastolicBp = 98,
                pulseRate = 62,
                totalCholesterol = 270,
                ldlCholesterol = 185,
                hdlCholesterol = 32,
                triglycerides = 265,
                fastingBloodSugar = 175,
                cardiacTroponinI = 0.88f,
                ckMb = 18.2f,
                stElevation = true,
                stElevationLeads = setOf(EcgLead.LEAD_II, EcgLead.LEAD_III, EcgLead.AVF, EcgLead.V3, EcgLead.V4),
                tWaveInversion = true,
                leftVentricularHypertrophy = true,
                pathologicalQWaves = true,
                ejectionFraction = 38,
                rwmaRegions = setOf(RwmaRegion.INFERIOR, RwmaRegion.SEPTAL),
                smoker = true,
                diabetic = true,
                hypertensive = true,
                typicalAngina = true
            )
        ),
        ClinicalCasePreset(
            id = "case_lcx_angina",
            title = "Case 3: Circumflex Ischemia (Severe LCX)",
            subtitle = "59F, Exertional dyspnea, Lateral T-wave Inversion, RWMA Lateral",
            badge = "LCX Stenosis",
            data = PatientClinicalData(
                name = "Case 3: Circumflex Angina (LCX)",
                age = 59,
                isMale = false,
                bmi = 27.1f,
                systolicBp = 148,
                diastolicBp = 90,
                pulseRate = 82,
                totalCholesterol = 230,
                ldlCholesterol = 152,
                hdlCholesterol = 44,
                triglycerides = 175,
                fastingBloodSugar = 118,
                cardiacTroponinI = 0.22f,
                ckMb = 7.1f,
                stElevation = false,
                stElevationLeads = emptySet(),
                tWaveInversion = true,
                leftVentricularHypertrophy = false,
                pathologicalQWaves = false,
                ejectionFraction = 52,
                rwmaRegions = setOf(RwmaRegion.LATERAL),
                smoker = false,
                diabetic = true,
                hypertensive = true,
                typicalAngina = true
            )
        ),
        ClinicalCasePreset(
            id = "case_healthy_screening",
            title = "Case 4: Healthy Screening (Low Risk)",
            subtitle = "38M, Asymptomatic checkup, Normal ECG & Normal Echo",
            badge = "CAD Negative",
            data = PatientClinicalData(
                name = "Case 4: Healthy Screening",
                age = 38,
                isMale = true,
                bmi = 22.8f,
                systolicBp = 118,
                diastolicBp = 76,
                pulseRate = 68,
                totalCholesterol = 172,
                ldlCholesterol = 96,
                hdlCholesterol = 58,
                triglycerides = 95,
                fastingBloodSugar = 88,
                cardiacTroponinI = 0.01f,
                ckMb = 1.8f,
                stElevation = false,
                stElevationLeads = emptySet(),
                tWaveInversion = false,
                leftVentricularHypertrophy = false,
                pathologicalQWaves = false,
                ejectionFraction = 65,
                rwmaRegions = setOf(RwmaRegion.NONE),
                smoker = false,
                diabetic = false,
                hypertensive = false,
                typicalAngina = false
            )
        )
    )

    fun evaluateCadRisk(patient: PatientClinicalData): CadRiskAssessment {
        // 1. Calculate Vessel-Specific Probabilities and Stenosis Percentages
        val ladPrediction = calculateLadPrediction(patient)
        val lcxPrediction = calculateLcxPrediction(patient)
        val rcaPrediction = calculateRcaPrediction(patient)

        val vesselMap = mapOf(
            VesselType.LAD to ladPrediction,
            VesselType.LCX to lcxPrediction,
            VesselType.RCA to rcaPrediction
        )

        // Overall CAD status: dataset definition CAD is positive if >= 50% stenosis in at least 1 target vessel
        val isCadPositive = ladPrediction.isSignificantStenosis ||
                lcxPrediction.isSignificantStenosis ||
                rcaPrediction.isSignificantStenosis

        // Overall Cardiac Risk Calculation (0-100%)
        val framingham = computeFramingham10YrRisk(patient)
        val diamondForrester = computeDiamondForrester(patient)

        // Weighted aggregate probability
        val maxVesselStenosis = max(ladPrediction.estimatedStenosisPercent,
            max(lcxPrediction.estimatedStenosisPercent, rcaPrediction.estimatedStenosisPercent))

        val significantVesselCount = listOf(ladPrediction, lcxPrediction, rcaPrediction).count { it.isSignificantStenosis }

        var compositeRiskScore = (maxVesselStenosis * 0.55f +
                framingham * 1.5f +
                diamondForrester * 0.25f +
                (if (patient.cardiacTroponinI > 0.04f) 20f else 0f) +
                (if (patient.stElevation) 18f else 0f) +
                (if (patient.ejectionFraction < 50) (50 - patient.ejectionFraction) * 0.8f else 0f) +
                significantVesselCount * 6f).roundToInt()

        compositeRiskScore = compositeRiskScore.coerceIn(4, 99)

        val riskCategory = when {
            compositeRiskScore >= 75 || (isCadPositive && patient.cardiacTroponinI > 0.1f) -> RiskCategory.VERY_HIGH
            compositeRiskScore >= 50 || isCadPositive -> RiskCategory.HIGH
            compositeRiskScore >= 25 -> RiskCategory.MODERATE
            else -> RiskCategory.LOW
        }

        // Primary involved territory identification
        val primaryTerritory = when {
            ladPrediction.estimatedStenosisPercent >= max(lcxPrediction.estimatedStenosisPercent, rcaPrediction.estimatedStenosisPercent) && ladPrediction.estimatedStenosisPercent >= 50 ->
                "LAD Territory: Anterior wall, Interventricular Septum, and Apex"
            rcaPrediction.estimatedStenosisPercent >= lcxPrediction.estimatedStenosisPercent && rcaPrediction.estimatedStenosisPercent >= 50 ->
                "RCA Territory: Inferior Wall, Posterior Septum, and AV Nodal Perfusion"
            lcxPrediction.estimatedStenosisPercent >= 50 ->
                "LCX Territory: Lateral & Posterolateral Left Ventricular Wall"
            else -> "Diffuse minimal atherosclerotic burden without focal severe stenosis"
        }

        val keyDrivers = mutableListOf<String>()
        if (patient.cardiacTroponinI > 0.04f) keyDrivers.add("Elevated Cardiac Enzymes (Troponin I ${patient.cardiacTroponinI} ng/mL indicates myocardial injury)")
        if (patient.stElevation) keyDrivers.add("ECG ST Elevation in ${patient.stElevationLeads.joinToString { it.leadName }} (Acute transmural ischemia pattern)")
        if (patient.rwmaRegions.any { it != RwmaRegion.NONE }) keyDrivers.add("Echocardiographic RWMA in: ${patient.rwmaRegions.filter { it != RwmaRegion.NONE }.joinToString { it.displayName }}")
        if (patient.ldlCholesterol >= 160) keyDrivers.add("Atherogenic Dyslipidemia (LDL-C ${patient.ldlCholesterol} mg/dL)")
        if (patient.systolicBp >= 140) keyDrivers.add("Stage 2 Hypertension (Systolic ${patient.systolicBp} mmHg)")
        if (patient.diabetic) keyDrivers.add("Diabetes Mellitus accelerating vascular plaque progression")
        if (patient.smoker) keyDrivers.add("Active Tobacco use increasing endothelial dysfunction & thrombosis")
        if (patient.ejectionFraction < 50) keyDrivers.add("Impaired Left Ventricular Ejection Fraction (LVEF ${patient.ejectionFraction}%)")
        if (keyDrivers.isEmpty()) keyDrivers.add("Favorable cardiovascular risk profile; optimal biomarker levels")

        val nextSteps = mutableListOf<String>()
        if (patient.stElevation && patient.cardiacTroponinI > 0.04f) {
            nextSteps.add("Immediate Emergency Cardiac Catheterization / Primary PCI activation")
            nextSteps.add("Dual Antiplatelet Therapy (Aspirin + P2Y12 inhibitor) + Anticoagulation")
            nextSteps.add("Continuous 12-lead ECG and telemetry hemodynamic monitoring")
        } else if (isCadPositive) {
            nextSteps.add("Coronary CT Angiography (CCTA) or Invasive Coronary Angiography (ICA)")
            nextSteps.add("Guideline-Directed Medical Therapy (High-intensity Statin + ACEi/ARB + Beta-Blocker)")
            nextSteps.add("Cardiology / Heart Team consultation for revascularization evaluation (PCI vs CABG)")
        } else if (riskCategory == RiskCategory.MODERATE) {
            nextSteps.add("Coronary Artery Calcium (CAC) Scoring for precise risk stratification")
            nextSteps.add("Exercise ECG or Stress Echocardiography for inducible ischemia")
            nextSteps.add("Aggressive lifestyle modification, dietary counseling, and lipid optimization")
        } else {
            nextSteps.add("Routine periodic cardiovascular health screening per AHA/ACC guidelines")
            nextSteps.add("Maintain heart-healthy diet, regular aerobic exercise, and optimal BP control")
        }

        val summary = buildString {
            if (isCadPositive) {
                append("Patient exhibits significant Coronary Artery Disease (CAD Positive) with ${significantVesselCount} vessel(s) predicted to have ≥50% stenosis. ")
                val affected = vesselMap.filter { it.value.isSignificantStenosis }.keys.joinToString { it.code }
                append("Primary affected branches: $affected. ")
            } else {
                append("Patient is classified as CAD Negative (<50% stenosis across LAD, LCX, and RCA). ")
            }
            append("Overall cardiovascular risk score is $compositeRiskScore% (${riskCategory.label}). ")
            if (patient.rwmaRegions.any { it != RwmaRegion.NONE }) {
                append("Regional Wall Motion Abnormality (RWMA) strongly correlates with focal perfusion deficits in the ${primaryTerritory.split(":").firstOrNull() ?: "affected"} distribution.")
            }
        }

        return CadRiskAssessment(
            overallCadPositive = isCadPositive,
            overallRiskPercent = compositeRiskScore,
            riskCategory = riskCategory,
            vesselPredictions = vesselMap,
            framingham10YrScore = framingham,
            diamondForresterPretest = diamondForrester,
            primaryInvolvedTerritory = primaryTerritory,
            clinicalSummary = summary,
            keyRiskDrivers = keyDrivers,
            recommendedNextSteps = nextSteps
        )
    }

    private fun calculateLadPrediction(p: PatientClinicalData): VesselPrediction {
        // Features linked to LAD: Anterior/Apical ST elevation (V1-V4), Septal/Anterior/Apical RWMA, Troponin, Age, Male, LDL, Smoking
        var logOdds = -3.2f

        // Clinical features
        logOdds += (p.age - 50) * 0.045f
        if (p.isMale) logOdds += 0.35f
        if (p.smoker) logOdds += 0.55f
        if (p.diabetic) logOdds += 0.65f
        if (p.hypertensive) logOdds += 0.50f
        if (p.typicalAngina) logOdds += 0.70f
        logOdds += ((p.ldlCholesterol - 100).coerceAtLeast(0)) * 0.012f

        // Cardiac enzymes
        if (p.cardiacTroponinI > 0.04f) logOdds += 1.4f
        if (p.cardiacTroponinI > 0.5f) logOdds += 1.2f

        // ECG features for LAD (Anterior leads V1-V4, Septal)
        val anteriorStLeads = p.stElevationLeads.count { it in listOf(EcgLead.V1, EcgLead.V2, EcgLead.V3, EcgLead.V4) }
        if (anteriorStLeads > 0) logOdds += 1.6f + anteriorStLeads * 0.45f
        if (p.pathologicalQWaves && anteriorStLeads > 0) logOdds += 1.1f
        if (p.leftVentricularHypertrophy) logOdds += 0.4f

        // Echo RWMA (Anterior, Septal, Apical)
        if (p.rwmaRegions.contains(RwmaRegion.ANTERIOR)) logOdds += 1.9f
        if (p.rwmaRegions.contains(RwmaRegion.SEPTAL)) logOdds += 1.4f
        if (p.rwmaRegions.contains(RwmaRegion.APICAL)) logOdds += 1.2f
        if (p.ejectionFraction < 50) logOdds += (50 - p.ejectionFraction) * 0.05f

        val prob = sigmoid(logOdds)
        val stenosisPercent = computeStenosisPercent(prob, baseSeverity = if (anteriorStLeads >= 2 || p.rwmaRegions.contains(RwmaRegion.ANTERIOR)) 72 else 30)

        val severity = when {
            stenosisPercent >= 70 -> VesselSeverity.SEVERE_CRITICAL
            stenosisPercent >= 50 -> VesselSeverity.MODERATE
            stenosisPercent >= 30 -> VesselSeverity.MILD
            else -> VesselSeverity.HEALTHY
        }

        return VesselPrediction(
            vessel = VesselType.LAD,
            stenosisProbability = prob,
            estimatedStenosisPercent = stenosisPercent,
            isSignificantStenosis = stenosisPercent >= 50,
            severity = severity,
            ischemicTerritory = "Anteroseptal wall, anterior LV free wall, apex",
            hemodynamicImpact = if (stenosisPercent >= 50) "Critical reduction in anterior LV perfusion; high risk of pump failure" else "Adequate anterior wall perfusion at baseline"
        )
    }

    private fun calculateLcxPrediction(p: PatientClinicalData): VesselPrediction {
        // Features linked to LCX: Lateral ST/T changes (I, aVL, V5-V6), Lateral RWMA, Dyslipidemia, Diabetes, Hypertension
        var logOdds = -3.5f

        logOdds += (p.age - 50) * 0.04f
        if (p.isMale) logOdds += 0.25f
        if (p.smoker) logOdds += 0.45f
        if (p.diabetic) logOdds += 0.70f
        if (p.hypertensive) logOdds += 0.45f
        if (p.typicalAngina) logOdds += 0.55f
        logOdds += ((p.totalCholesterol - 180).coerceAtLeast(0)) * 0.010f
        logOdds += ((p.triglycerides - 150).coerceAtLeast(0)) * 0.006f

        if (p.cardiacTroponinI > 0.04f) logOdds += 0.9f

        // ECG Lateral leads (I, aVL, V5, V6)
        val lateralStLeads = p.stElevationLeads.count { it in listOf(EcgLead.LEAD_I, EcgLead.AVL, EcgLead.V5, EcgLead.V6) }
        if (lateralStLeads > 0) logOdds += 1.7f + lateralStLeads * 0.4f
        if (p.tWaveInversion) logOdds += 0.9f

        // Echo RWMA Lateral
        if (p.rwmaRegions.contains(RwmaRegion.LATERAL)) logOdds += 2.2f

        val prob = sigmoid(logOdds)
        val stenosisPercent = computeStenosisPercent(prob, baseSeverity = if (lateralStLeads >= 2 || p.rwmaRegions.contains(RwmaRegion.LATERAL)) 68 else 28)

        val severity = when {
            stenosisPercent >= 70 -> VesselSeverity.SEVERE_CRITICAL
            stenosisPercent >= 50 -> VesselSeverity.MODERATE
            stenosisPercent >= 30 -> VesselSeverity.MILD
            else -> VesselSeverity.HEALTHY
        }

        return VesselPrediction(
            vessel = VesselType.LCX,
            stenosisProbability = prob,
            estimatedStenosisPercent = stenosisPercent,
            isSignificantStenosis = stenosisPercent >= 50,
            severity = severity,
            ischemicTerritory = "Lateral and posterolateral left ventricular free wall",
            hemodynamicImpact = if (stenosisPercent >= 50) "Impaired lateral wall contractility; potential papillary muscle ischemia" else "Preserved lateral LV perfusion"
        )
    }

    private fun calculateRcaPrediction(p: PatientClinicalData): VesselPrediction {
        // Features linked to RCA: Inferior ST elevation (II, III, aVF), Inferior RWMA, Bradycardia (AV node), Smoking, Age
        var logOdds = -3.4f

        logOdds += (p.age - 50) * 0.042f
        if (p.isMale) logOdds += 0.40f
        if (p.smoker) logOdds += 0.65f
        if (p.diabetic) logOdds += 0.55f
        if (p.hypertensive) logOdds += 0.48f
        if (p.typicalAngina) logOdds += 0.60f
        logOdds += ((p.ldlCholesterol - 100).coerceAtLeast(0)) * 0.010f

        // Bradycardia link (RCA supplies AV node in ~90% right-dominant hearts)
        if (p.pulseRate < 60) logOdds += 0.85f

        if (p.cardiacTroponinI > 0.04f) logOdds += 1.0f

        // ECG Inferior leads (II, III, aVF)
        val inferiorStLeads = p.stElevationLeads.count { it in listOf(EcgLead.LEAD_II, EcgLead.LEAD_III, EcgLead.AVF) }
        if (inferiorStLeads > 0) logOdds += 1.9f + inferiorStLeads * 0.5f

        // Echo RWMA Inferior
        if (p.rwmaRegions.contains(RwmaRegion.INFERIOR)) logOdds += 2.3f

        val prob = sigmoid(logOdds)
        val stenosisPercent = computeStenosisPercent(prob, baseSeverity = if (inferiorStLeads >= 2 || p.rwmaRegions.contains(RwmaRegion.INFERIOR)) 70 else 28)

        val severity = when {
            stenosisPercent >= 70 -> VesselSeverity.SEVERE_CRITICAL
            stenosisPercent >= 50 -> VesselSeverity.MODERATE
            stenosisPercent >= 30 -> VesselSeverity.MILD
            else -> VesselSeverity.HEALTHY
        }

        return VesselPrediction(
            vessel = VesselType.RCA,
            stenosisProbability = prob,
            estimatedStenosisPercent = stenosisPercent,
            isSignificantStenosis = stenosisPercent >= 50,
            severity = severity,
            ischemicTerritory = "Inferior LV wall, right ventricle, posterior interventricular septum",
            hemodynamicImpact = if (stenosisPercent >= 50) "High risk of conduction disturbances (AV block) and right ventricular dysfunction" else "Normal right coronary & inferior perfusion"
        )
    }

    private fun sigmoid(x: Float): Float {
        return (1.0 / (1.0 + exp(-x.toDouble()))).toFloat().coerceIn(0.01f, 0.99f)
    }

    private fun computeStenosisPercent(prob: Float, baseSeverity: Int): Int {
        // Non-linear calibration mapping probability to 0-100% stenosis
        val calc = when {
            prob >= 0.85f -> 75 + ((prob - 0.85f) / 0.15f * 23).roundToInt()
            prob >= 0.55f -> 50 + ((prob - 0.55f) / 0.30f * 24).roundToInt()
            prob >= 0.30f -> 30 + ((prob - 0.30f) / 0.25f * 19).roundToInt()
            else -> (prob / 0.30f * 29).roundToInt()
        }
        return calc.coerceIn(5, 99)
    }

    private fun computeFramingham10YrRisk(p: PatientClinicalData): Float {
        var points = 0
        // Age
        points += when {
            p.age >= 70 -> 12
            p.age >= 60 -> 9
            p.age >= 50 -> 6
            p.age >= 40 -> 3
            else -> 0
        }
        // Total Cholesterol
        points += when {
            p.totalCholesterol >= 240 -> 3
            p.totalCholesterol >= 200 -> 2
            p.totalCholesterol >= 160 -> 1
            else -> 0
        }
        // Smoker
        if (p.smoker) points += 4
        // HDL
        points += when {
            p.hdlCholesterol < 35 -> 2
            p.hdlCholesterol < 45 -> 1
            p.hdlCholesterol >= 60 -> -1
            else -> 0
        }
        // Systolic BP
        points += when {
            p.systolicBp >= 160 -> 4
            p.systolicBp >= 140 -> 3
            p.systolicBp >= 130 -> 2
            p.systolicBp >= 120 -> 1
            else -> 0
        }
        if (p.diabetic) points += 3

        val risk = (points * 2.2f).coerceIn(1.0f, 65.0f)
        return risk
    }

    private fun computeDiamondForrester(p: PatientClinicalData): Float {
        // Pre-test CAD probability based on age, sex, symptom type
        var prob = if (p.typicalAngina) {
            when {
                p.age >= 60 -> if (p.isMale) 93f else 78f
                p.age >= 50 -> if (p.isMale) 87f else 62f
                p.age >= 40 -> if (p.isMale) 72f else 46f
                else -> if (p.isMale) 50f else 28f
            }
        } else {
            when {
                p.age >= 60 -> if (p.isMale) 56f else 35f
                p.age >= 50 -> if (p.isMale) 41f else 20f
                else -> if (p.isMale) 22f else 10f
            }
        }
        return prob
    }
}
