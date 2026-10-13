package com.example.model

import kotlin.math.roundToInt

enum class StatinIntensity(val label: String, val ldlReductionFactor: Float) {
    NONE("No Statin", 0.0f),
    MODERATE("Moderate-Intensity (-30% LDL)", 0.30f),
    HIGH_INTENSITY("High-Intensity (-50% LDL)", 0.50f)
}

data class WhatIfParameters(
    val sodiumReductionPercent: Int = 0, // 0 to 50%
    val cardioHoursPerWeek: Float = 0f, // 0 to 7 hours
    val statinTherapy: StatinIntensity = StatinIntensity.NONE,
    val quitSmoking: Boolean = false,
    val weightLossPercent: Int = 0 // 0 to 20%
) {
    val isModified: Boolean
        get() = sodiumReductionPercent > 0 ||
                cardioHoursPerWeek > 0f ||
                statinTherapy != StatinIntensity.NONE ||
                quitSmoking ||
                weightLossPercent > 0
}

object WhatIfEngine {

    fun applySimulation(
        base: PatientClinicalData,
        params: WhatIfParameters
    ): PatientClinicalData {
        if (!params.isModified) return base

        // 1. Dietary Sodium reduction -> drops BP
        val sbpDropFromSodium = (params.sodiumReductionPercent / 50f * 14f).roundToInt()
        val dbpDropFromSodium = (params.sodiumReductionPercent / 50f * 8f).roundToInt()

        // 2. Cardio exercise -> lowers resting HR, drops SBP, raises HDL, lowers triglycerides, improves LVEF
        val cardioFactor = (params.cardioHoursPerWeek / 7f).coerceIn(0f, 1f)
        val hrDropFromCardio = (cardioFactor * 12f).roundToInt()
        val sbpDropFromCardio = (cardioFactor * 8f).roundToInt()
        val hdlGainFromCardio = (cardioFactor * 8f).roundToInt()
        val trigDropFromCardio = (cardioFactor * 45f).roundToInt()
        val lvefGainFromCardio = (cardioFactor * 5f).roundToInt()

        // 3. Weight loss -> drops BMI, FBS, and SBP
        val weightFactor = (params.weightLossPercent / 20f).coerceIn(0f, 1f)
        val bmiDrop = base.bmi * (params.weightLossPercent / 100f)
        val fbsDrop = (weightFactor * 25f).roundToInt()
        val sbpDropFromWeight = (weightFactor * 6f).roundToInt()

        // 4. Statin therapy -> reduces LDL-C
        val ldlDrop = (base.ldlCholesterol * params.statinTherapy.ldlReductionFactor).roundToInt()

        // 5. Smoking cessation
        val isStillSmoker = if (params.quitSmoking) false else base.smoker

        val newSbp = (base.systolicBp - sbpDropFromSodium - sbpDropFromCardio - sbpDropFromWeight).coerceAtLeast(95)
        val newDbp = (base.diastolicBp - dbpDropFromSodium).coerceAtLeast(60)
        val newHr = (base.pulseRate - hrDropFromCardio).coerceAtLeast(50)
        val newLdl = (base.ldlCholesterol - ldlDrop).coerceAtLeast(40)
        val newHdl = (base.hdlCholesterol + hdlGainFromCardio).coerceAtMost(85)
        val newTrig = (base.triglycerides - trigDropFromCardio).coerceAtLeast(60)
        val newFbs = (base.fastingBloodSugar - fbsDrop).coerceAtLeast(75)
        val newBmi = (base.bmi - bmiDrop).coerceAtLeast(19.0f)
        val newLvef = (base.ejectionFraction + lvefGainFromCardio).coerceAtMost(70)

        return base.copy(
            name = "${base.name} (What-If Simulated)",
            systolicBp = newSbp,
            diastolicBp = newDbp,
            pulseRate = newHr,
            ldlCholesterol = newLdl,
            hdlCholesterol = newHdl,
            triglycerides = newTrig,
            fastingBloodSugar = newFbs,
            bmi = (newBmi * 10).roundToInt() / 10f,
            ejectionFraction = newLvef,
            smoker = isStillSmoker
        )
    }
}
