package com.example

import com.example.ml.CardioRiskEngine
import com.example.model.StatinIntensity
import com.example.model.VesselType
import com.example.model.WhatIfEngine
import com.example.model.WhatIfParameters
import com.example.xai.XaiEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testAnteriorStemiLadStenosis() {
    val preset = CardioRiskEngine.SAMPLE_PRESETS.first { it.id == "case_lad_stemi" }
    val assessment = CardioRiskEngine.evaluateCadRisk(preset.data)

    assertTrue("Overall CAD should be positive for Anterior STEMI", assessment.overallCadPositive)
    val lad = assessment.vesselPredictions[VesselType.LAD]
    assertNotNull("LAD prediction must exist", lad)
    assertTrue("LAD stenosis must be >= 50%", lad!!.isSignificantStenosis)
    assertTrue("LAD stenosis should be severe (>= 70%)", lad.estimatedStenosisPercent >= 70)
  }

  @Test
  fun testHealthyScreeningCase() {
    val preset = CardioRiskEngine.SAMPLE_PRESETS.first { it.id == "case_healthy_screening" }
    val assessment = CardioRiskEngine.evaluateCadRisk(preset.data)

    assertFalse("Overall CAD should be negative for healthy case", assessment.overallCadPositive)
    val lad = assessment.vesselPredictions[VesselType.LAD]!!
    val lcx = assessment.vesselPredictions[VesselType.LCX]!!
    val rca = assessment.vesselPredictions[VesselType.RCA]!!

    assertTrue("LAD should be < 50%", lad.estimatedStenosisPercent < 50)
    assertTrue("LCX should be < 50%", lcx.estimatedStenosisPercent < 50)
    assertTrue("RCA should be < 50%", rca.estimatedStenosisPercent < 50)
  }

  @Test
  fun testXaiFeatureAttribution() {
    val preset = CardioRiskEngine.SAMPLE_PRESETS.first { it.id == "case_lad_stemi" }
    val assessment = CardioRiskEngine.evaluateCadRisk(preset.data)
    val attributions = XaiEngine.computeFeatureAttributions(preset.data, assessment)

    assertFalse("Attributions should not be empty", attributions.isEmpty())
    val top = attributions.first()
    assertTrue("Top feature should be primary driver", top.isPrimaryDriver)
    assertTrue("Top driver should have positive attribution points", top.attributionPoints > 0)
  }

  @Test
  fun testWhatIfSimulationLifestyleImpact() {
    val base = CardioRiskEngine.SAMPLE_PRESETS.first { it.id == "case_lcx_angina" }.data

    val whatIf = WhatIfEngine.applySimulation(
      base,
      WhatIfParameters(
        sodiumReductionPercent = 40,
        cardioHoursPerWeek = 4.5f,
        statinTherapy = StatinIntensity.HIGH_INTENSITY,
        quitSmoking = true
      )
    )

    assertTrue("Simulated systolic BP should be lower", whatIf.systolicBp < base.systolicBp)
    assertTrue("Simulated resting HR should be lower", whatIf.pulseRate < base.pulseRate)
    assertTrue("Simulated LDL should be lower", whatIf.ldlCholesterol < base.ldlCholesterol)
    assertTrue("Simulated HDL should be higher", whatIf.hdlCholesterol > base.hdlCholesterol)
  }
}
