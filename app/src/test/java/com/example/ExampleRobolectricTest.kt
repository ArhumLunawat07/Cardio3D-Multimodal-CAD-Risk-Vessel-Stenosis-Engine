package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ml.CardioRiskEngine
import com.example.xai.XaiEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CardioVision 3D", appName)
  }

  @Test
  fun `test EcgJson parsing in Android environment`() {
    val base = CardioRiskEngine.SAMPLE_PRESETS.first().data
    val json = XaiEngine.generateSampleEcgJson(base)
    val parsed = XaiEngine.parseEcgJson(json)

    assertTrue("ECG JSON parsing should succeed", parsed.isSuccess)
    val telemetry = parsed.getOrNull()
    assertNotNull(telemetry)
    assertEquals(base.pulseRate, telemetry!!.heartRateBpm)
  }
}
