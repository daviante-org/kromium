package org.daviante.kromium.api.error

import org.daviante.kromium.api.config.KromiumClientConfig
import org.daviante.kromium.api.ui.KromiumUiFramework
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KromiumExceptionTest {

    @Test
    fun testFrameworkRequiredException() {
        val ex = KromiumException.FrameworkRequired()
        assertEquals(KromiumErrorCode.FRAMEWORK_REQUIRED, ex.errorCode)
        assertFalse(ex.isRecoverable)
        assertEquals("UI framework is mandatory for OSR browser creation. No default OSR framework is permitted.", ex.message)
    }

    @Test
    fun testCanvasNotAvailableException() {
        val ex = KromiumException.CanvasNotAvailable(KromiumUiFramework.JAVAFX)
        assertEquals(KromiumErrorCode.CANVAS_NOT_AVAILABLE, ex.errorCode)
        assertEquals(KromiumUiFramework.JAVAFX, ex.framework)
        assertFalse(ex.isRecoverable)
        assertEquals("No OSR canvas factory registered for framework: JAVAFX. Please ensure kromium-javafx is on the classpath.", ex.message)
    }

    @Test
    fun testClientConfigFrameworkDefaultIsNull() {
        val defaultConfig = KromiumClientConfig.default()
        assertNull(defaultConfig.framework)

        val builtConfig = KromiumClientConfig.builder().build()
        assertNull(builtConfig.framework)

        val swingConfig = KromiumClientConfig.builder().framework(KromiumUiFramework.SWING).build()
        assertEquals(KromiumUiFramework.SWING, swingConfig.framework)

        val stringConfig = KromiumClientConfig.builder().framework("compose").build()
        assertEquals(KromiumUiFramework.COMPOSE, stringConfig.framework)
    }

    @Test
    fun testFromStringThrowsFrameworkRequiredOnNullOrBlank() {
        assertFailsWith<KromiumException.FrameworkRequired> {
            KromiumUiFramework.fromString(null)
        }
        assertFailsWith<KromiumException.FrameworkRequired> {
            KromiumUiFramework.fromString("   ")
        }
    }

    @Test
    fun testFromStringThrowsInvalidConfigOnUnknown() {
        val ex = assertFailsWith<KromiumException.InvalidConfig> {
            KromiumUiFramework.fromString("unknown_framework")
        }
        assertTrue(ex.message!!.contains("Unknown UI framework 'unknown_framework'"))
    }
}
