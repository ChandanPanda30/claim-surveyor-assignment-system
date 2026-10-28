package com.claimsurveyor.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

/**
 * Failure screenshot mechanism: when a UI test fails, save a PNG of the browser and the page HTML
 * to target/screenshots/ before the browser is closed.
 */
public class ScreenshotOnFailure implements AfterTestExecutionCallback {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        Object instance = context.getRequiredTestInstance();
        if (!(instance instanceof BaseSeleniumTest test) || test.driver == null) {
            return;
        }

        Path dir = Path.of("target", "screenshots");
        Files.createDirectories(dir);
        String name = instance.getClass().getSimpleName() + "_"
                + context.getRequiredTestMethod().getName() + "_" + LocalDateTime.now().format(STAMP);

        byte[] png = ((TakesScreenshot) test.driver).getScreenshotAs(OutputType.BYTES);
        Path pngFile = dir.resolve(name + ".png");
        Files.write(pngFile, png);
        Files.writeString(dir.resolve(name + ".html"), test.driver.getPageSource());

        System.out.println("[selenium] Test failed - screenshot saved: " + pngFile.toAbsolutePath());
    }
}
