package com.claimsurveyor.ui;

import com.claimsurveyor.ClaimSurveyorApplication;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Starts the real application on a random port and gives each test its own Chrome browser.
 * Run headless by default; use -Dselenium.headless=false to watch the browser.
 */
@SpringBootTest(classes = ClaimSurveyorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(ScreenshotOnFailure.class)
abstract class BaseSeleniumTest {

    @LocalServerPort
    private int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (!"false".equalsIgnoreCase(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1366,900");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------- small helpers shared by the journeys ----------

    protected void open(String path) {
        driver.get("http://localhost:" + port + path);
    }

    protected WebElement find(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    protected String text(String id) {
        return find(id).getText().trim();
    }

    protected void waitForText(String id, String expected) {
        wait.until(ExpectedConditions.textToBe(By.id(id), expected));
    }

    protected void type(String id, String value) {
        WebElement element = find(id);
        element.clear();
        element.sendKeys(value);
    }

    protected void click(String id) {
        wait.until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
    }

    protected boolean exists(String id) {
        return !driver.findElements(By.id(id)).isEmpty();
    }

    /** Date inputs are locale-sensitive when typed, so set the ISO value directly. */
    protected void setDate(String id, LocalDate date) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];", find(id), date.toString());
    }

    /** Fills the claim form with valid data except the policy number and incident date, which tests choose. */
    protected void fillClaimForm(String policyNumber, LocalDate incidentDate) {
        open("/claims/new");
        type("policyNumber", policyNumber);
        type("claimantName", "Asha Rao");
        type("claimantEmail", "asha.rao@example.com");
        type("claimantPhone", "9876543210");
        new Select(find("claimType")).selectByValue("MOTOR");
        setDate("incidentDate", incidentDate);
        type("incidentLocation", "Andheri, Mumbai");
        type("estimatedLoss", "45000");
        type("description", "Rear bumper damaged in a parking collision.");
    }

    /** Submits a valid claim through the UI and returns its claim number. */
    protected String submitValidClaim() {
        fillClaimForm("POL-123456", LocalDate.now().minusDays(3));
        click("submit-claim");
        return text("claim-number");
    }
}
