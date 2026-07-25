package com.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FormatHelperTest {

    @Test
    fun evaluateExpression_basic() {
        assertEquals(500000.0, FormatHelper.evaluateExpression("500000"), 0.0)
        assertEquals(500000.0, FormatHelper.evaluateExpression(" 500000 "), 0.0)
    }

    @Test
    fun evaluateExpression_operators() {
        assertEquals(150000.0, FormatHelper.evaluateExpression("100000+50000"), 0.0)
        assertEquals(50000.0, FormatHelper.evaluateExpression("100000-50000"), 0.0)
        assertEquals(200000.0, FormatHelper.evaluateExpression("100000*2"), 0.0)
        assertEquals(50000.0, FormatHelper.evaluateExpression("100000/2"), 0.0)
    }

    @Test
    fun evaluateExpression_complex() {
        assertEquals(250000.0, FormatHelper.evaluateExpression("100000+50000*3"), 0.0)
        assertEquals(0.0, FormatHelper.evaluateExpression("invalid"), 0.0)
        assertEquals(40000.0, FormatHelper.evaluateExpression(" 100000 - 20000 * 3 "), 0.0)
    }
}
