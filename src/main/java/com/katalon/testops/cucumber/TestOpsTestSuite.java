package com.katalon.testops.cucumber;

import io.cucumber.messages.Messages;

import java.util.Collections;
import java.util.List;

public class TestOpsTestSuite {
    private List<TestOpsTestCase> testCases;
    private Messages.GherkinDocument document;
    private String uuid;

    public TestOpsTestSuite(Messages.GherkinDocument document) {
        this.document = document;
    }

    public List<TestOpsTestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestOpsTestCase> testCases) {
        this.testCases = Collections.unmodifiableList(testCases);
    }

    public Messages.GherkinDocument getDocument() {
        return document;
    }

    public String getName() {
        return document.getFeature().getName();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
