package com.claimsurveyor.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Demonstrates the failure screenshot mechanism. It is skipped normally (no browser is even started)
 * and only fails when run with -Dselenium.demoFailure=true, so the build stays green by default.
 */
class ScreenshotMechanismIT extends BaseSeleniumTest {

    @Test
    @EnabledIfSystemProperty(named = "selenium.demoFailure", matches = "true")
    void failsOnPurposeToProduceAScreenshot() {
        open("/");

        assertThat(driver.getTitle()).isEqualTo("This title is deliberately wrong");
    }
}
