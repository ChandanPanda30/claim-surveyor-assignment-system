# UI Test Plan (Selenium)

## Scope
End-to-end browser tests for the Claim Surveyor Assignment System MVP. The tests start the real application
on a random port with an in-memory database, then drive it through Chrome with Selenium WebDriver.

| Item | Choice |
|------|--------|
| Tool | Selenium WebDriver 4.25 (Java) with JUnit 5 and AssertJ |
| Browser | Chrome, headless by default (Selenium Manager downloads the driver automatically) |
| Runner | Maven Failsafe (`*IT.java`), run by `mvn verify` |
| Test data | Created through the UI in each test, so tests do not depend on each other |
| Failure evidence | Screenshot (PNG) and page HTML saved to `target/screenshots/` |

## Critical user journeys

| ID | Journey | Key assertions |
|----|---------|----------------|
| UI-1 | Claimant submits a valid claim | Claim number matches `CLM-yyyy-nnnnn`; confirmation message shown; status "Submitted"; no surveyor yet |
| UI-2 | Invalid data is rejected | Empty form shows required-field errors; bad policy number and future incident date are rejected; no claim is created |
| UI-3 | Reviewer assigns surveyor, uploads document, approves | Status becomes "Under review" with a surveyor; uploaded file listed; status becomes "Approved" |
| UI-4 | Rejection needs a reason | Rejecting without remarks shows an error and keeps "Under review"; with remarks status becomes "Rejected" and remarks are shown |
| UI-5 | Track a claim by number | Known number shows its status; unknown number shows "No claim found" |

Test data used: policy `POL-123456`, claimant "Asha Rao", mobile `9876543210`, claim type Motor,
estimated loss 45000, incident date 3 days ago, invalid policy `12345`, future date today + 10 days,
a small PNG file created on the fly for the upload.

## How to run

```bash
mvn verify                                  # unit tests + Selenium tests
mvn verify "-Dselenium.headless=false"      # watch the browser while the tests run
mvn verify "-Dselenium.demoFailure=true"    # deliberately fail one test to produce a screenshot
```

Run only the UI tests: `mvn verify "-Dit.test=ClaimJourneysIT"`

## Reports and evidence
- Console summary at the end of `mvn verify`
- `target/failsafe-reports/` (text and XML per test class; Jenkins reads the XML in Task 10)
- Optional HTML report: `mvn org.apache.maven.plugins:maven-surefire-report-plugin:3.2.5:failsafe-report-only`
  then open `target/site/failsafe-report.html`
- Failure screenshots: `target/screenshots/<TestClass>_<test>_<timestamp>.png` and `.html`

## Entry and exit criteria
- Entry: application builds and unit tests pass.
- Exit: all five journeys pass; any failure has a screenshot attached.
