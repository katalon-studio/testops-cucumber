package com.katalon.testops.cucumber;

import com.katalon.testops.commons.ReportLifecycle;
import com.katalon.testops.commons.helper.GeneratorHelper;
import com.katalon.testops.commons.model.TestSuite;
import com.katalon.testops.cucumber.helper.ReportHelper;
import io.cucumber.messages.Messages;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.util.*;

public class ReportListener implements ConcurrentEventListener {

    ReportLifecycle reportLifecycle;
    List<TestOpsTestSuite> testSuites;

    public ReportListener() {
        reportLifecycle = new ReportLifecycle();
        testSuites = new ArrayList<>();
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunStarted.class, this::testRunStarted);
        eventPublisher.registerHandlerFor(TestRunFinished.class, this::TestRunFinished);
        eventPublisher.registerHandlerFor(TestSourceRead.class, this::TestSourceRead);
        eventPublisher.registerHandlerFor(TestCaseStarted.class, this::testCaseStarted);
        eventPublisher.registerHandlerFor(TestCaseFinished.class, this::TestCaseFinished);
    }

    private void testRunStarted(TestRunStarted e) {
        reportLifecycle.startExecution();
        reportLifecycle.writeMetadata(ReportHelper.createMetadata());
    }

    private void TestRunFinished(TestRunFinished e) {
        reportLifecycle.stopExecution();
        reportLifecycle.writeTestResultsReport();
        reportLifecycle.writeTestSuitesReport();
        reportLifecycle.writeExecutionReport();
        reportLifecycle.upload();
        reportLifecycle.reset();
    }

    private void TestSourceRead(TestSourceRead e) {
        try {
            Optional<Messages.GherkinDocument> opDocument = ReportHelper.parseDocument(e);
            if (!opDocument.isPresent()) {
                return;
            }
            Messages.GherkinDocument document = opDocument.get();
            testSuites.add(buildTestSuite(document));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void testCaseStarted(TestCaseStarted e) {
        TestCase testCase = e.getTestCase();
        Optional<TestOpsTestSuite> opTestSuite = getTestSuite(testCase);
        if (!opTestSuite.isPresent()) {
            return;
        }
        TestOpsTestSuite testSuite = opTestSuite.get();
        if (shouldCreateTestSuite(testSuite)) {
            startTestSuite(testSuite);
        }
        Optional<TestOpsTestCase> opTestCase = getTestOpsTestCase(testCase);
        if (!opTestCase.isPresent()) {
            return;
        }
        opTestCase.get().setExecuted(true);
        reportLifecycle.startTestCase();
    }

    private void startTestSuite(TestOpsTestSuite testSuite) {
        TestSuite suite = new TestSuite();
        suite.setName(testSuite.getName());
        reportLifecycle.startSuite(suite, testSuite.getUuid());
    }

    private boolean shouldCreateTestSuite(TestOpsTestSuite testSuite) {
        return testSuite.getTestCases().stream().allMatch(tc -> !tc.isExecuted() && !tc.isSkipped());
    }

    private boolean shouldEndTestSuite(TestOpsTestSuite testSuite) {
        return testSuite.getTestCases().stream().allMatch(tc -> tc.isExecuted() || tc.isSkipped());
    }

    private void TestCaseFinished(TestCaseFinished e) {
        Optional<TestOpsTestCase> testCase = getTestOpsTestCase(e.getTestCase());
        if (!testCase.isPresent()) {
            return;
        }
        Optional<TestOpsTestSuite> testSuite = getTestSuite(e.getTestCase());
        if (!testSuite.isPresent()) {
            return;
        }
        handleSkipTestCases(testCase.get(), testSuite.get());
        reportLifecycle.stopTestCase(ReportHelper.createTestResult(testCase.get(), e.getResult(), testSuite.get()));
        if (shouldEndTestSuite(testSuite.get())) {
            reportLifecycle.stopTestSuite(testSuite.get().getUuid());
        }
    }

    private synchronized void handleSkipTestCases(TestOpsTestCase testCase, TestOpsTestSuite testSuite) {
        List<TestOpsTestCase> testCases = testSuite.getTestCases();
        int index = testCases.indexOf(testCase);
        if (index <= 0) {
            return;
        }
        int i = index - 1;
        TestOpsTestCase tmpTestCase = testCases.get(i);
        while (!tmpTestCase.isExecuted() && !tmpTestCase.isSkipped() && i > 0) {
            tmpTestCase.setSkipped(true);
            skipTestCase(tmpTestCase, testSuite);
            --i;
        }
    }

    private void skipTestCase(TestOpsTestCase testCase, TestOpsTestSuite testSuite) {
        reportLifecycle.stopTestCase(ReportHelper.createTestResult(testCase, null, testSuite));
    }

    private TestOpsTestSuite buildTestSuite(Messages.GherkinDocument document) {
        List<TestOpsTestCase> testCases = new ArrayList<>();
        TestOpsTestSuite testOpsTestSuite = new TestOpsTestSuite(document);
        testOpsTestSuite.setUuid(GeneratorHelper.generateUniqueValue());
        document.getFeature().getChildrenList().forEach(featureChild -> {
            TestOpsTestCase testCase = new TestOpsTestCase();
            testCase.setScenario(featureChild.getScenario());
            testCase.setParent(testOpsTestSuite);
            testCases.add(testCase);
        });
        testOpsTestSuite.setTestCases(testCases);
        return testOpsTestSuite;
    }

    private Optional<TestOpsTestSuite> getTestSuite(TestCase testCase) {
        return testSuites.stream().
                filter(ts -> ts.getDocument().getUri().equals(testCase.getUri().toString())).
                findFirst();
    }

    private Optional<TestOpsTestCase> getTestOpsTestCase(TestCase testCase) {
        Optional<TestOpsTestSuite> testSuite = getTestSuite(testCase);
        if (!testSuite.isPresent()) {
            return Optional.empty();
        }
        Optional<TestOpsTestCase> opTestCase = testSuite.get().getTestCases().stream()
                .filter(tc -> isTestCase(tc, testCase))
                .findFirst();
        if (opTestCase.isPresent() && Objects.isNull(opTestCase.get().getTestCase())) {
            opTestCase.get().setTestCase(testCase);
        }
        return opTestCase;
    }

    private boolean isTestCase(TestOpsTestCase testOpsTestCase, TestCase testCase) {
        Messages.GherkinDocument.Feature.Scenario scenario = testOpsTestCase.getScenario();
        return scenario.getName().equals(testCase.getName())
                && scenario.getLocation().getColumn() == testCase.getLocation().getColumn()
                && scenario.getLocation().getLine() == testCase.getLocation().getLine();
    }

}
