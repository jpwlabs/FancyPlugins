package com.fancyinnovations.fancydialogs.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FDFeatureFlagsTest {

    @Test
    void jpwPolicyAlwaysDisablesUpstreamAutomaticDialogs() {
        FDFeatureFlags.DISABLE_WELCOME_DIALOG.setEnabled(false);
        FDFeatureFlags.DISABLE_QUICK_ACTIONS_DIALOG.setEnabled(false);

        FDFeatureFlags.enforceJpwPresentationPolicy();

        assertTrue(FDFeatureFlags.DISABLE_WELCOME_DIALOG.isEnabled());
        assertTrue(FDFeatureFlags.DISABLE_QUICK_ACTIONS_DIALOG.isEnabled());
    }
}
