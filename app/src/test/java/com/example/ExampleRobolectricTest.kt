package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.math.MathProblemGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertEquals("Smart Alarm", appName)
    }

    @Test
    fun `test math problem generator produces valid equations`() {
        val easyProblem = MathProblemGenerator.generate("EASY")
        assertNotNull(easyProblem.expression)
        assertTrue(easyProblem.expression.isNotEmpty())

        val mediumProblem = MathProblemGenerator.generate("MEDIUM")
        assertNotNull(mediumProblem.expression)

        val hardProblem = MathProblemGenerator.generate("HARD")
        assertNotNull(hardProblem.expression)
    }
}
