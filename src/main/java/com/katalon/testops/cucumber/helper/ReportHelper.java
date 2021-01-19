package com.katalon.testops.cucumber.helper;

import com.katalon.testops.commons.helper.GeneratorHelper;
import com.katalon.testops.commons.model.Metadata;
import com.katalon.testops.commons.model.Status;
import com.katalon.testops.commons.model.TestResult;
import com.katalon.testops.cucumber.ReportListener;
import com.katalon.testops.cucumber.TestOpsTestCase;
import com.katalon.testops.cucumber.TestOpsTestSuite;
import io.cucumber.gherkin.Gherkin;
import io.cucumber.messages.Messages;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.TestSourceRead;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReportHelper {
    public static Optional<Messages.GherkinDocument> parseDocument(TestSourceRead event) {
        final List<Messages.Envelope> sources = Collections.singletonList(Gherkin.makeSourceEnvelope(event.getSource(), event.getUri().toString()));
        final List<Messages.Envelope> envelopes = Gherkin.fromSources(
                sources,
                true,
                true,
                true,
                () -> String.valueOf(GeneratorHelper.generateUniqueValue())).collect(Collectors.toList());
        Optional<Messages.Envelope> envelop = envelopes.stream().filter(envelope -> envelope.hasGherkinDocument()).findFirst();
        if (envelop.isPresent()) {
            return Optional.of(envelop.get().getGherkinDocument());
        }
        return Optional.empty();
    }

    public static Metadata createMetadata() {
        Metadata metadata = new Metadata();
        metadata.setFramework("cucumber6");
        metadata.setLanguage("java");
        metadata.setVersion(ReportListener.class.getPackage().getImplementationVersion());
        return metadata;
    }

    public static TestResult createTestResult(TestOpsTestCase testCase, Result result, TestOpsTestSuite testSuite) {
        String uuid = GeneratorHelper.generateUniqueValue();
        TestResult testResult = new TestResult();
        testResult.setUuid(uuid);
        if (testCase.isSkipped()) {
            testResult.setStatus(Status.SKIPPED);
        } else {
            testResult.setStatus(getStatus(result));
        }
        testResult.setName(testCase.getName());
        testResult.setSuiteName(testSuite.getName());
        testResult.setParentUuid(testSuite.getUuid());
        if (testResult.getStatus() != Status.PASSED) {
            if (testCase.isSkipped()) {
                testResult.addFailure("Skipped unconditionally", "");
            } else {
                if (testResult.getStatus() == Status.SKIPPED) {
                    testResult.addFailure(result.getError().getMessage(), "");
                }
                testResult.addError(result.getError());
            }
        }
        return testResult;
    }

    private static Status getStatus(Result result) {
        switch (result.getStatus()) {
            case PASSED:
                return Status.PASSED;
            case FAILED:
                return Status.FAILED;
            case SKIPPED:
            case UNUSED:
            case UNDEFINED:
                return Status.SKIPPED;
            case PENDING:
            default:
                return Status.INCOMPLETE;
        }
    }

}
