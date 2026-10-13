package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiCardioService
import com.example.data.db.AssessmentRecordEntity
import com.example.data.db.CardioDatabase
import com.example.ml.CardioRiskEngine
import com.example.model.*
import com.example.pdf.PdfReportGenerator
import com.example.xai.EcgTelemetryData
import com.example.xai.FeatureAttribution
import com.example.xai.XaiEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class CardioTab(val title: String) {
    VISUALIZER_3D("3D Heart"),
    CLINICAL_DATA("Patient Data"),
    XAI_DASHBOARD("Explainable AI"),
    AI_CONSULTATION("AI Synthesis"),
    CHATBOT("Cardio Chat"),
    SAVED_RECORDS("History")
}

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val responseText: String) : AiState()
    data class Error(val errorMessage: String) : AiState()
}

sealed class ImageGenState {
    object Idle : ImageGenState()
    object Loading : ImageGenState()
    data class Success(val bitmap: Bitmap) : ImageGenState()
    data class Error(val errorMessage: String) : ImageGenState()
}

class CardioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CardioDatabase.getDatabase(application)
    private val dao = db.assessmentDao()

    private val _patientData = MutableStateFlow(CardioRiskEngine.SAMPLE_PRESETS.first().data)
    val patientData: StateFlow<PatientClinicalData> = _patientData.asStateFlow()

    private val _assessment = MutableStateFlow(CardioRiskEngine.evaluateCadRisk(_patientData.value))
    val assessment: StateFlow<CadRiskAssessment> = _assessment.asStateFlow()

    // 3D Rendering Mode: true = WebGL Three.js, false = Native Vector Canvas
    private val _isThreeJsMode = MutableStateFlow(true)
    val isThreeJsMode: StateFlow<Boolean> = _isThreeJsMode.asStateFlow()

    // "What-If" Simulation Parameters
    private val _whatIfParams = MutableStateFlow(WhatIfParameters())
    val whatIfParams: StateFlow<WhatIfParameters> = _whatIfParams.asStateFlow()

    private val _isWhatIfActive = MutableStateFlow(false)
    val isWhatIfActive: StateFlow<Boolean> = _isWhatIfActive.asStateFlow()

    private val _simulatedPatient = MutableStateFlow(_patientData.value)
    val simulatedPatient: StateFlow<PatientClinicalData> = _simulatedPatient.asStateFlow()

    private val _simulatedAssessment = MutableStateFlow(_assessment.value)
    val simulatedAssessment: StateFlow<CadRiskAssessment> = _simulatedAssessment.asStateFlow()

    // The active patient and assessment (reflects What-If if active)
    val activePatient: StateFlow<PatientClinicalData> = combine(_patientData, _simulatedPatient, _isWhatIfActive) { base, sim, active ->
        if (active) sim else base
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _patientData.value)

    val activeAssessment: StateFlow<CadRiskAssessment> = combine(_assessment, _simulatedAssessment, _isWhatIfActive) { base, sim, active ->
        if (active) sim else base
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _assessment.value)

    // XAI Feature Attributions
    val featureAttributions: StateFlow<List<FeatureAttribution>> = activePatient.map { pat ->
        val assess = CardioRiskEngine.evaluateCadRisk(pat)
        XaiEngine.computeFeatureAttributions(pat, assess)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Raw ECG JSON representation
    private val _rawEcgJson = MutableStateFlow(XaiEngine.generateSampleEcgJson(_patientData.value))
    val rawEcgJson: StateFlow<String> = _rawEcgJson.asStateFlow()

    private val _selectedVessel = MutableStateFlow<VesselType?>(VesselType.LAD)
    val selectedVessel: StateFlow<VesselType?> = _selectedVessel.asStateFlow()

    private val _showRwmaOverlay = MutableStateFlow(true)
    val showRwmaOverlay: StateFlow<Boolean> = _showRwmaOverlay.asStateFlow()

    private val _currentTab = MutableStateFlow(CardioTab.VISUALIZER_3D)
    val currentTab: StateFlow<CardioTab> = _currentTab.asStateFlow()

    // AI States
    private val _deepThinkingState = MutableStateFlow<AiState>(AiState.Idle)
    val deepThinkingState: StateFlow<AiState> = _deepThinkingState.asStateFlow()

    private val _guidelineSearchState = MutableStateFlow<AiState>(AiState.Idle)
    val guidelineSearchState: StateFlow<AiState> = _guidelineSearchState.asStateFlow()

    private val _fastTriageState = MutableStateFlow<AiState>(AiState.Idle)
    val fastTriageState: StateFlow<AiState> = _fastTriageState.asStateFlow()

    private val _imageGenState = MutableStateFlow<ImageGenState>(ImageGenState.Idle)
    val imageGenState: StateFlow<ImageGenState> = _imageGenState.asStateFlow()

    // Chatbot States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "model",
                content = "Welcome to CardioVision 3D. I am your AI Cardiology Consultant. You can ask me about coronary anatomy (LAD, LCX, RCA), ischemia mechanisms, echocardiographic RWMA correlation, or treatment planning based on the current patient's clinical inputs."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _selectedChatModel = MutableStateFlow(GeminiCardioService.MODEL_SEARCH_FLASH)
    val selectedChatModel: StateFlow<String> = _selectedChatModel.asStateFlow()

    // Room database history
    val savedRecords: StateFlow<List<AssessmentRecordEntity>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _saveNotification = MutableStateFlow<String?>(null)
    val saveNotification: StateFlow<String?> = _saveNotification.asStateFlow()

    init {
        // Automatically re-evaluate whenever patient data changes
        viewModelScope.launch {
            _patientData.collect { newData ->
                _assessment.value = CardioRiskEngine.evaluateCadRisk(newData)
                _rawEcgJson.value = XaiEngine.generateSampleEcgJson(newData)
                recomputeSimulation()
            }
        }
    }

    fun setTab(tab: CardioTab) {
        _currentTab.value = tab
    }

    fun toggleRendererMode() {
        _isThreeJsMode.value = !_isThreeJsMode.value
    }

    fun selectVessel(vessel: VesselType?) {
        _selectedVessel.value = vessel
    }

    fun toggleRwmaOverlay() {
        _showRwmaOverlay.value = !_showRwmaOverlay.value
    }

    fun loadPreset(preset: ClinicalCasePreset) {
        _patientData.value = preset.data
        resetWhatIf()
        _selectedVessel.value = when (preset.id) {
            "case_lad_stemi" -> VesselType.LAD
            "case_multivessel" -> VesselType.RCA
            "case_lcx_angina" -> VesselType.LCX
            else -> null
        }
    }

    fun updatePatient(updater: (PatientClinicalData) -> PatientClinicalData) {
        _patientData.value = updater(_patientData.value)
    }

    // What-If Simulation Controls
    fun toggleWhatIfSimulation(active: Boolean) {
        _isWhatIfActive.value = active
        if (active) recomputeSimulation()
    }

    fun updateWhatIf(updater: (WhatIfParameters) -> WhatIfParameters) {
        _whatIfParams.value = updater(_whatIfParams.value)
        _isWhatIfActive.value = true
        recomputeSimulation()
    }

    fun resetWhatIf() {
        _whatIfParams.value = WhatIfParameters()
        _isWhatIfActive.value = false
        _simulatedPatient.value = _patientData.value
        _simulatedAssessment.value = _assessment.value
    }

    fun applyWhatIfToBaseline() {
        if (_isWhatIfActive.value && _whatIfParams.value.isModified) {
            _patientData.value = _simulatedPatient.value
            resetWhatIf()
            _saveNotification.value = "What-If simulated lifestyle changes applied to patient baseline!"
        }
    }

    private fun recomputeSimulation() {
        val simPatient = WhatIfEngine.applySimulation(_patientData.value, _whatIfParams.value)
        _simulatedPatient.value = simPatient
        _simulatedAssessment.value = CardioRiskEngine.evaluateCadRisk(simPatient)
    }

    // PDF Report Export
    fun exportPdfReport(context: Context): File? {
        val pat = activePatient.value
        val assess = activeAssessment.value
        val file = PdfReportGenerator.generateAndShareReport(context, pat, assess)
        if (file != null) {
            _saveNotification.value = "Clinical PDF Report generated & shared successfully!"
        }
        return file
    }

    // ECG JSON import
    fun updateRawEcgJson(newJson: String) {
        _rawEcgJson.value = newJson
        val result = XaiEngine.parseEcgJson(newJson)
        result.onSuccess { telemetry ->
            // Map telemetry leads to PatientClinicalData
            val leads = mutableSetOf<EcgLead>()
            telemetry.stElevationMap.forEach { (leadStr, mv) ->
                if (mv >= 0.1) {
                    val matching = EcgLead.values().firstOrNull { it.leadName.equals(leadStr, ignoreCase = true) }
                    if (matching != null) leads.add(matching)
                }
            }
            val hasSt = leads.isNotEmpty()
            val hasTInv = telemetry.tWaveInversionLeads.isNotEmpty()
            val hasQ = telemetry.pathologicalQWaveLeads.isNotEmpty()

            updatePatient { p ->
                p.copy(
                    pulseRate = telemetry.heartRateBpm,
                    stElevation = hasSt,
                    stElevationLeads = leads,
                    tWaveInversion = hasTInv,
                    pathologicalQWaves = hasQ
                )
            }
            _saveNotification.value = "ECG JSON telemetry parsed: HR ${telemetry.heartRateBpm} bpm, ST leads: ${leads.joinToString { it.leadName }}"
        }
    }

    fun runDeepThinkingAnalysis(query: String = "Perform comprehensive cardiovascular risk synthesis and anatomical vessel correlation.") {
        viewModelScope.launch {
            _deepThinkingState.value = AiState.Loading
            val pat = activePatient.value
            val assess = activeAssessment.value
            val result = GeminiCardioService.analyzeWithDeepThinking(pat, assess, query)
            _deepThinkingState.value = result.fold(
                onSuccess = { AiState.Success(it) },
                onFailure = { AiState.Error(it.message ?: "Deep thinking analysis failed.") }
            )
        }
    }

    fun searchGuidelines(query: String) {
        viewModelScope.launch {
            _guidelineSearchState.value = AiState.Loading
            val result = GeminiCardioService.searchCardiologyGuidelines(query)
            _guidelineSearchState.value = result.fold(
                onSuccess = { AiState.Success(it) },
                onFailure = { AiState.Error(it.message ?: "Guidelines search failed.") }
            )
        }
    }

    fun runFastTriage() {
        viewModelScope.launch {
            _fastTriageState.value = AiState.Loading
            val pat = activePatient.value
            val assess = activeAssessment.value
            val result = GeminiCardioService.fastTriageAssessment(pat, assess)
            _fastTriageState.value = result.fold(
                onSuccess = { AiState.Success(it) },
                onFailure = { AiState.Error(it.message ?: "Fast triage failed.") }
            )
        }
    }

    fun setChatModel(model: String) {
        _selectedChatModel.value = model
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text.trim()
        )
        val updated = _chatMessages.value + userMsg
        _chatMessages.value = updated
        _isChatLoading.value = true

        viewModelScope.launch {
            val result = GeminiCardioService.sendChatMessage(updated, _selectedChatModel.value)
            _isChatLoading.value = false
            result.fold(
                onSuccess = { reply ->
                    val modelMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "model",
                        content = reply,
                        modelTag = _selectedChatModel.value
                    )
                    _chatMessages.value = _chatMessages.value + modelMsg
                },
                onFailure = { err ->
                    val errorMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "model",
                        content = "Could not generate response: ${err.message}"
                    )
                    _chatMessages.value = _chatMessages.value + errorMsg
                }
            )
        }
    }

    fun generateIllustration(prompt: String) {
        viewModelScope.launch {
            _imageGenState.value = ImageGenState.Loading
            val result = GeminiCardioService.generateCardioIllustration(prompt)
            _imageGenState.value = result.fold(
                onSuccess = { ImageGenState.Success(it) },
                onFailure = { ImageGenState.Error(it.message ?: "Image generation failed.") }
            )
        }
    }

    fun saveCurrentAssessment() {
        viewModelScope.launch {
            val patient = activePatient.value
            val assess = activeAssessment.value
            val lad = assess.vesselPredictions[VesselType.LAD]?.estimatedStenosisPercent ?: 0
            val lcx = assess.vesselPredictions[VesselType.LCX]?.estimatedStenosisPercent ?: 0
            val rca = assess.vesselPredictions[VesselType.RCA]?.estimatedStenosisPercent ?: 0

            val record = AssessmentRecordEntity(
                patientName = patient.name,
                age = patient.age,
                isMale = patient.isMale,
                overallCadPositive = assess.overallCadPositive,
                overallRiskPercent = assess.overallRiskPercent,
                ladStenosisPercent = lad,
                lcxStenosisPercent = lcx,
                rcaStenosisPercent = rca,
                troponinI = patient.cardiacTroponinI,
                ejectionFraction = patient.ejectionFraction,
                primaryTerritory = assess.primaryInvolvedTerritory,
                clinicalSummary = assess.clinicalSummary
            )
            dao.insertRecord(record)
            _saveNotification.value = "Assessment archived to local clinical records."
        }
    }

    fun clearSaveNotification() {
        _saveNotification.value = null
    }

    fun deleteRecord(record: AssessmentRecordEntity) {
        viewModelScope.launch {
            dao.deleteRecord(record)
        }
    }
}
