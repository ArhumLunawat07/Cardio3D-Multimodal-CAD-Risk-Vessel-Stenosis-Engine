package com.example.model

enum class VesselType(val code: String, val fullName: String, val territory: String) {
    LAD("LAD", "Left Anterior Descending", "Anterior wall, interventricular septum, apex"),
    LCX("LCX", "Left Circumflex", "Lateral and posterior left ventricular wall"),
    RCA("RCA", "Right Coronary Artery", "Inferior wall, right ventricle, posterior septum, SA/AV nodes")
}

enum class RwmaRegion(val displayName: String, val linkedVessel: VesselType) {
    ANTERIOR("Anterior", VesselType.LAD),
    SEPTAL("Septal", VesselType.LAD),
    APICAL("Apical", VesselType.LAD),
    LATERAL("Lateral", VesselType.LCX),
    INFERIOR("Inferior", VesselType.RCA),
    NONE("None (Normal Wall Motion)", VesselType.LAD)
}

enum class VesselSeverity(val label: String) {
    HEALTHY("Minimal (<30%)"),
    MILD("Mild (30–49%)"),
    MODERATE("Moderate (50–69%)"),
    SEVERE_CRITICAL("Severe (≥70%)")
}

enum class RiskCategory(val label: String) {
    LOW("Low Cardiac Risk"),
    MODERATE("Moderate Risk"),
    HIGH("High Risk"),
    VERY_HIGH("Critical / Very High Risk")
}

enum class EcgLead(val leadName: String, val territory: String) {
    V1("V1", "Septal"),
    V2("V2", "Septal"),
    V3("V3", "Anterior"),
    V4("V4", "Anterior"),
    V5("V5", "Apical/Lateral"),
    V6("V6", "Apical/Lateral"),
    LEAD_I("I", "High Lateral"),
    AVL("aVL", "High Lateral"),
    LEAD_II("II", "Inferior"),
    LEAD_III("III", "Inferior"),
    AVF("aVF", "Inferior")
}

data class PatientClinicalData(
    val name: String = "Patient Assessment #104",
    val age: Int = 62,
    val isMale: Boolean = true,
    val bmi: Float = 28.4f,
    // Vitals
    val systolicBp: Int = 145,
    val diastolicBp: Int = 92,
    val pulseRate: Int = 78,
    // Laboratory Biomarkers
    val totalCholesterol: Int = 224, // mg/dL
    val ldlCholesterol: Int = 142,   // mg/dL
    val hdlCholesterol: Int = 38,    // mg/dL
    val triglycerides: Int = 190,   // mg/dL
    val fastingBloodSugar: Int = 126, // mg/dL
    val cardiacTroponinI: Float = 0.18f, // ng/mL (normal < 0.04)
    val ckMb: Float = 6.4f, // ng/mL (normal < 5.0)
    // ECG Features
    val stElevation: Boolean = true,
    val stElevationLeads: Set<EcgLead> = setOf(EcgLead.V2, EcgLead.V3, EcgLead.V4),
    val tWaveInversion: Boolean = false,
    val leftVentricularHypertrophy: Boolean = true,
    val pathologicalQWaves: Boolean = false,
    // Echocardiogram
    val ejectionFraction: Int = 45, // %
    val rwmaRegions: Set<RwmaRegion> = setOf(RwmaRegion.ANTERIOR, RwmaRegion.APICAL),
    // Clinical History
    val smoker: Boolean = true,
    val diabetic: Boolean = true,
    val hypertensive: Boolean = true,
    val typicalAngina: Boolean = true
)

data class VesselPrediction(
    val vessel: VesselType,
    val stenosisProbability: Float, // 0.0 to 1.0
    val estimatedStenosisPercent: Int, // 0 to 100%
    val isSignificantStenosis: Boolean, // >= 50%
    val severity: VesselSeverity,
    val ischemicTerritory: String,
    val hemodynamicImpact: String
)

data class CadRiskAssessment(
    val overallCadPositive: Boolean, // >= 50% in at least one vessel
    val overallRiskPercent: Int, // 0 to 100%
    val riskCategory: RiskCategory,
    val vesselPredictions: Map<VesselType, VesselPrediction>,
    val framingham10YrScore: Float,
    val diamondForresterPretest: Float,
    val primaryInvolvedTerritory: String,
    val clinicalSummary: String,
    val keyRiskDrivers: List<String>,
    val recommendedNextSteps: List<String>
)

data class ClinicalCasePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val data: PatientClinicalData
)
