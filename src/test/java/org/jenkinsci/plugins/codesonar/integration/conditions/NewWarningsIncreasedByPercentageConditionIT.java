package org.jenkinsci.plugins.codesonar.integration.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.NewWarningsIncreasedByPercentageCondition;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.junit.jupiter.api.Test;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NewWarningsIncreasedByPercentageConditionIT extends ConditionIntegrationTestBase {

    @Test
    void percentageOfBrandNewWarningsIsAboveTheThreshold_BuildIsSetToWarrantedResult() throws Exception {
        // arrange
        final String WARRANTED_RESULT = Result.FAILURE.toString();
        final Result EXPECTED_RESULT = Result.fromString(WARRANTED_RESULT);

        final float PERCENTAGE = 10.0f;

        NewWarningsIncreasedByPercentageCondition condition
                = new NewWarningsIncreasedByPercentageCondition(Float.toString(PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setHttpService(mockedHttpService);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }

    @Test
    void percentageOfBrandNewWarningsIsBellowtheThreshold_BuildIsSuccessful() throws Exception {
        // arrange
        final String WARRANTED_RESULT = Result.FAILURE.toString();
        final Result EXPECTED_RESULT = Result.SUCCESS;

        final float PERCENTAGE = 70.0f;

        NewWarningsIncreasedByPercentageCondition condition
                = new NewWarningsIncreasedByPercentageCondition(Float.toString(PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setAuthenticationService(mockedAuthenticationService);
        codeSonarPublisher.setHttpService(mockedHttpService);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }
}
