package com.katalon.testops.cucumber;

import io.cucumber.messages.Messages;
import io.cucumber.plugin.event.TestCase;

public class TestOpsTestCase {
    private TestCase testCase;
    private TestOpsTestSuite parent;
    private Messages.GherkinDocument.Feature.Scenario scenario;
    private boolean isExecuted;
    private boolean isSkipped;

    public TestOpsTestCase() {
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    public void setExecuted(boolean executed) {
        isExecuted = executed;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public String getName() {
        if (testCase != null) {
            return testCase.getName();
        }
        return scenario.getName();
    }

    public TestOpsTestSuite getParent() {
        return this.parent;
    }

    public void setParent(TestOpsTestSuite parent) {
        this.parent = parent;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    public Messages.GherkinDocument.Feature.Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Messages.GherkinDocument.Feature.Scenario scenario) {
        this.scenario = scenario;
    }

    public boolean isSkipped() {
        return isSkipped;
    }

    public void setSkipped(boolean skipped) {
        isSkipped = skipped;
    }
}
