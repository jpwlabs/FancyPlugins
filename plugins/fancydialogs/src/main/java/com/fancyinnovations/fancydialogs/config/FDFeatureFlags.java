package com.fancyinnovations.fancydialogs.config;

import com.fancyinnovations.config.featureflags.FeatureFlag;
import com.fancyinnovations.config.featureflags.FeatureFlagConfig;
import com.fancyinnovations.fancydialogs.FancyDialogsPlugin;

public class FDFeatureFlags {

    public static final FeatureFlag DEBUG_MODE = new FeatureFlag("debug-mode", "Enable debug mode", false);
    public static final FeatureFlag DISABLE_WELCOME_DIALOG = new FeatureFlag("disable-welcome-dialog", "Disable showing a welcome dialog when a player joins for the first time", false);
    public static final FeatureFlag DISABLE_QUICK_ACTIONS_DIALOG = new FeatureFlag("disable-quick-actions-dialog", "Disable the quick actions dialog", false);

    public static void load() {
        FeatureFlagConfig config = new FeatureFlagConfig(FancyDialogsPlugin.get());
        config.addFeatureFlag(DEBUG_MODE);
        config.addFeatureFlag(DISABLE_WELCOME_DIALOG);
        config.addFeatureFlag(DISABLE_QUICK_ACTIONS_DIALOG);
        config.load();
        enforceJpwPresentationPolicy();
    }

    /** MegaMine owns first-join and quick-action presentation; upstream defaults may never pre-empt it. */
    static void enforceJpwPresentationPolicy() {
        DISABLE_WELCOME_DIALOG.setEnabled(true);
        DISABLE_QUICK_ACTIONS_DIALOG.setEnabled(true);
    }

}
