package dev.frostguard.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigurationKeyEnumTest {

    @Test
    void disablesAnalyticsUntilTheUserOptsIn() {
        assertEquals("false", ConfigurationKeyEnum.ANALYTICS_ENABLED_BOOL.getDefaultValue());
        assertEquals("0", ConfigurationKeyEnum.ANALYTICS_CONSENT_VERSION_INT.getDefaultValue());
    }

    @Test
    void retiredIntelEraSettingRemainsReadableButIsNotExposed() {
        assertTrue(ConfigurationKeyEnum.INTEL_FC_ERA_BOOL.isLegacyOnly());
        assertFalse(ConfigurationKeyEnum.byCategory(ConfigurationKeyEnum.ConfigCategory.INTEL)
                .contains(ConfigurationKeyEnum.INTEL_FC_ERA_BOOL));
    }
}
