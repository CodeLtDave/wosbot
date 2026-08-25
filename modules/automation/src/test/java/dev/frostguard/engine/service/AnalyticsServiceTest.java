package dev.frostguard.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {

    @Test
    void describesDeliveryFailureWithoutStackTrace() {
        Exception failure = new SocketTimeoutException("Connect timed out");

        assertEquals("Connect timed out", AnalyticsService.describeDeliveryFailure(failure));
    }

    @Test
    void fallsBackToExceptionTypeWhenDeliveryFailureHasNoMessage() {
        Exception failure = new SocketTimeoutException();

        assertEquals("SocketTimeoutException", AnalyticsService.describeDeliveryFailure(failure));
    }

    @Test
    void requiresExplicitVersionedConsentForAnalytics() {
        assertFalse(AnalyticsService.hasExplicitAnalyticsConsent(null));
        assertFalse(AnalyticsService.hasExplicitAnalyticsConsent(Map.of()));
        assertFalse(AnalyticsService.hasExplicitAnalyticsConsent(Map.of(
                "ANALYTICS_ENABLED_BOOL", "true")));
        assertTrue(AnalyticsService.hasExplicitAnalyticsConsent(Map.of(
                "ANALYTICS_ENABLED_BOOL", "true",
                "ANALYTICS_CONSENT_VERSION_INT", "1")));
    }
}
