package com.jarvis.aegis.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreflightReportTest {
    @Test fun warningsDoNotBlockActivation() {
        val report = PreflightReport(listOf(PreflightCheck("notification", "Notification", CheckSeverity.WARNING, "Optional")))
        assertTrue(report.canActivate)
    }

    @Test fun anyBlockerPreventsActivation() {
        val report = PreflightReport(
            listOf(
                PreflightCheck("storage", "Storage", CheckSeverity.PASS, "Ready"),
                PreflightCheck("accessibility", "Accessibility", CheckSeverity.BLOCKER, "Disabled"),
            ),
        )
        assertFalse(report.canActivate)
    }
}
