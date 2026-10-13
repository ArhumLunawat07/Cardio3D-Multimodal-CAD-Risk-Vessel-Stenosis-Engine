package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.example.model.CadRiskAssessment
import com.example.model.PatientClinicalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelTag: String? = null
)

object GeminiCardioService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Recommended models per guidelines
    const val MODEL_DEEP_THINKING = "gemini-3.1-pro-preview"
    const val MODEL_SEARCH_FLASH = "gemini-3.5-flash"
    const val MODEL_FAST_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_IMAGE = "gemini-nano-banana-2.1"
    const val MODEL_IMAGE_FALLBACK = "gemini-2.5-flash-image"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Deep Thinking mode using gemini-3.1-pro-preview with thinkingLevel HIGH.
     * Do not set maxOutputTokens.
     */
    suspend fun analyzeWithDeepThinking(
        patient: PatientClinicalData,
        assessment: CadRiskAssessment,
        query: String = "Perform comprehensive cardiovascular risk synthesis and anatomical vessel correlation."
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."))
        }

        try {
            val root = JSONObject()

            // System instruction
            val sysInstruction = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text",
                    "You are a Senior Academic Interventional Cardiologist. Analyze patient physiological data, ECG leads, biomarkers, echocardiographic RWMA (Regional Wall Motion Abnormalities), and 3D coronary anatomy focusing on the three target vessels: LAD (Left Anterior Descending), LCX (Left Circumflex), and RCA (Right Coronary Artery). Provide deep clinical pathophysiology, coronary anatomy justification, and tailored guideline-directed recommendations."
                )))
            }
            root.put("systemInstruction", sysInstruction)

            // Contents
            val promptText = buildClinicalPrompt(patient, assessment, query)
            val contents = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", promptText)))
                }
            )
            root.put("contents", contents)

            // Generation config with ThinkingLevel HIGH - NO maxOutputTokens
            val genConfig = JSONObject().apply {
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                put("thinkingConfig", thinkingConfig)
                put("temperature", 0.7)
            }
            root.put("generationConfig", genConfig)

            val url = "${BASE_URL}${MODEL_DEEP_THINKING}:generateContent?key=$apiKey"
            val body = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error (${response.code}): $respBody"))
            }

            val text = parseResponseText(respBody)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search Grounded model using gemini-3.5-flash with googleSearch tool.
     */
    suspend fun searchCardiologyGuidelines(
        query: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        try {
            val root = JSONObject()

            val sysInstruction = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text",
                    "You are an evidence-based Cardiology Information Specialist. Use Google Search grounding to cite the latest ACC/AHA, ESC (European Society of Cardiology) guidelines, clinical trials, and pharmacology protocols for coronary artery disease and myocardial ischemia."
                )))
            }
            root.put("systemInstruction", sysInstruction)

            val contents = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", query)))
                }
            )
            root.put("contents", contents)

            // Add Google Search Tool
            val tools = JSONArray().put(
                JSONObject().apply {
                    put("googleSearch", JSONObject())
                }
            )
            root.put("tools", tools)

            val url = "${BASE_URL}${MODEL_SEARCH_FLASH}:generateContent?key=$apiKey"
            val body = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error (${response.code}): $respBody"))
            }

            val text = parseResponseText(respBody)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Low-latency rapid triage response using gemini-3.1-flash-lite-preview.
     */
    suspend fun fastTriageAssessment(
        patient: PatientClinicalData,
        assessment: CadRiskAssessment
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        try {
            val root = JSONObject()
            val prompt = """
                RAPID CARDIOLOGY TRIAGE REQUEST:
                - Patient: ${patient.age}yo ${if (patient.isMale) "M" else "F"}, BP ${patient.systolicBp}/${patient.diastolicBp}, HR ${patient.pulseRate}
                - Troponin I: ${patient.cardiacTroponinI} ng/mL | ST Elevation: ${patient.stElevation} in ${patient.stElevationLeads.joinToString { it.leadName }}
                - Echo RWMA: ${patient.rwmaRegions.joinToString { it.displayName }} | LVEF: ${patient.ejectionFraction}%
                - Predicted Stenosis: LAD ${assessment.vesselPredictions[com.example.model.VesselType.LAD]?.estimatedStenosisPercent}%, LCX ${assessment.vesselPredictions[com.example.model.VesselType.LCX]?.estimatedStenosisPercent}%, RCA ${assessment.vesselPredictions[com.example.model.VesselType.RCA]?.estimatedStenosisPercent}%
                - Overall CAD Positive: ${assessment.overallCadPositive} (Risk: ${assessment.overallRiskPercent}%)

                Provide a 3-bullet ultra-fast urgent emergency triage recommendation with immediate bedside actions.
            """.trimIndent()

            val contents = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            )
            root.put("contents", contents)

            val url = "${BASE_URL}${MODEL_FAST_LITE}:generateContent?key=$apiKey"
            val body = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error (${response.code}): $respBody"))
            }

            val text = parseResponseText(respBody)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Multi-turn chat with conversation history and system instruction.
     */
    suspend fun sendChatMessage(
        messages: List<ChatMessage>,
        selectedModel: String = MODEL_SEARCH_FLASH
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        try {
            val root = JSONObject()

            val sysInstruction = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text",
                    "You are 'CardioVision AI', an expert cardiology clinical decision support assistant. You specialize in Coronary Artery Disease (CAD), coronary vessel anatomy (LAD, LCX, RCA), myocardial infarction, echocardiographic regional wall motion abnormalities (RWMA), and ECG interpretation. Be concise, highly professional, empathetic, and scientifically accurate. Always format answers with clear headers, bullet points, and clinical reasoning."
                )))
            }
            root.put("systemInstruction", sysInstruction)

            val contentsArray = JSONArray()
            // Append up to the last 10 messages for context
            val recent = messages.takeLast(10)
            recent.forEach { msg ->
                val roleStr = if (msg.role == "model") "model" else "user"
                contentsArray.put(JSONObject().apply {
                    put("role", roleStr)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }
            root.put("contents", contentsArray)

            // If selectedModel is MODEL_DEEP_THINKING, configure thinkingLevel HIGH
            if (selectedModel == MODEL_DEEP_THINKING) {
                val genConfig = JSONObject().apply {
                    put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
                }
                root.put("generationConfig", genConfig)
            } else if (selectedModel == MODEL_SEARCH_FLASH) {
                val tools = JSONArray().put(JSONObject().put("googleSearch", JSONObject()))
                root.put("tools", tools)
            }

            val targetModel = when (selectedModel) {
                MODEL_DEEP_THINKING -> MODEL_DEEP_THINKING
                MODEL_FAST_LITE -> MODEL_FAST_LITE
                else -> MODEL_SEARCH_FLASH
            }

            val url = "${BASE_URL}${targetModel}:generateContent?key=$apiKey"
            val body = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error (${response.code}): $respBody"))
            }

            val text = parseResponseText(respBody)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create image illustration using gemini-nano-banana-2.1 or gemini-2.5-flash-image
     */
    suspend fun generateCardioIllustration(
        prompt: String
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        // Try gemini-nano-banana-2.1 first, fall back to gemini-2.5-flash-image
        val modelsToTry = listOf(MODEL_IMAGE, MODEL_IMAGE_FALLBACK)
        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                val root = JSONObject()
                val contents = JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }
                )
                root.put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", "1:1")
                        put("imageSize", "1K")
                    })
                    put("responseModalities", JSONArray().put("IMAGE"))
                }
                root.put("generationConfig", genConfig)

                val url = "${BASE_URL}${model}:generateContent?key=$apiKey"
                val body = root.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = okHttpClient.newCall(request).execute()
                val respBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val bitmap = parseImageResponse(respBody)
                    if (bitmap != null) {
                        return@withContext Result.success(bitmap)
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Could not generate image"))
    }

    private fun buildClinicalPrompt(
        patient: PatientClinicalData,
        assessment: CadRiskAssessment,
        userQuery: String
    ): String {
        val lad = assessment.vesselPredictions[com.example.model.VesselType.LAD]
        val lcx = assessment.vesselPredictions[com.example.model.VesselType.LCX]
        val rca = assessment.vesselPredictions[com.example.model.VesselType.RCA]

        return """
            PATIENT CLINICAL PROFILE:
            - Age: ${patient.age} years | Sex: ${if (patient.isMale) "Male" else "Female"} | BMI: ${patient.bmi}
            - Vitals: BP ${patient.systolicBp}/${patient.diastolicBp} mmHg | Pulse: ${patient.pulseRate} bpm
            - Biomarkers: Total Chol: ${patient.totalCholesterol} mg/dL | LDL: ${patient.ldlCholesterol} mg/dL | HDL: ${patient.hdlCholesterol} mg/dL | Triglycerides: ${patient.triglycerides} mg/dL | FBS: ${patient.fastingBloodSugar} mg/dL
            - Cardiac Enzymes: Troponin I: ${patient.cardiacTroponinI} ng/mL (ref <0.04) | CK-MB: ${patient.ckMb} ng/mL
            - ECG Findings: ST Elevation: ${patient.stElevation} (Leads: ${patient.stElevationLeads.joinToString { it.leadName }}), T-wave Inversion: ${patient.tWaveInversion}, LVH: ${patient.leftVentricularHypertrophy}, Q-Waves: ${patient.pathologicalQWaves}
            - Echocardiogram: LVEF: ${patient.ejectionFraction}%, Regional Wall Motion Abnormality (RWMA): ${patient.rwmaRegions.joinToString { it.displayName }}
            - Risk Factors: Tobacco: ${patient.smoker}, Diabetes: ${patient.diabetic}, Hypertension: ${patient.hypertensive}, Typical Angina: ${patient.typicalAngina}

            MULTIVARIATE VESSEL STENOSIS PREDICTIONS:
            - LAD (Left Anterior Descending): ${lad?.estimatedStenosisPercent}% stenosis (Prob: ${((lad?.stenosisProbability ?: 0f) * 100).toInt()}%) -> Severity: ${lad?.severity?.label}
            - LCX (Left Circumflex): ${lcx?.estimatedStenosisPercent}% stenosis (Prob: ${((lcx?.stenosisProbability ?: 0f) * 100).toInt()}%) -> Severity: ${lcx?.severity?.label}
            - RCA (Right Coronary Artery): ${rca?.estimatedStenosisPercent}% stenosis (Prob: ${((rca?.stenosisProbability ?: 0f) * 100).toInt()}%) -> Severity: ${rca?.severity?.label}
            - Overall CAD Status (>=50% in any target vessel): ${if (assessment.overallCadPositive) "POSITIVE" else "NEGATIVE"}
            - Overall Composite Cardiac Risk: ${assessment.overallRiskPercent}% (${assessment.riskCategory.label})
            - Framingham 10-Year Score: ${assessment.framingham10YrScore}% | Diamond-Forrester Pre-Test: ${assessment.diamondForresterPretest}%

            CLINICIAN QUERY:
            $userQuery
        """.trimIndent()
    }

    private fun parseResponseText(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return "No response generated."
            if (candidates.length() == 0) return "No response candidates returned."
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return "Empty content."
            val parts = content.optJSONArray("parts") ?: return "No parts in content."

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                if (text.isNotEmpty()) sb.append(text)
            }
            if (sb.isEmpty()) "Empty response text." else sb.toString()
        } catch (e: Exception) {
            "Parsing error: ${e.message}"
        }
    }

    private fun parseImageResponse(jsonStr: String): Bitmap? {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data", "")
                    if (base64Data.isNotEmpty()) {
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
