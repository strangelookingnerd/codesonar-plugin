package org.jenkinsci.plugins.codesonar.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 *
 * @author Andrius
 */
@WithJenkins
class CodeSonarPublisherIT {

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void providedHubAddressIsEmpty_BuildFails() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition =
                new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));

        final String EMPTY_HUB_ADDRESS = "";
        final String VALID_PROJECT_NAME = "projectName";
        final String VALID_CODESONAR_PROJECT_FILE = "projectName.prj";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        List<Condition> conditions = new ArrayList<>();
        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", EMPTY_HUB_ADDRESS, VALID_PROJECT_NAME, "", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }

    @Test
    void providedProjectNameIsEmpty_BuildFails() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition =
                new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));

        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String EMPTY_PROJECT_NAME = "";
        final String VALID_CODESONAR_PROJECT_FILE = "projectName.prj";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        List<Condition> conditions = new ArrayList<>();
        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS, EMPTY_PROJECT_NAME,"", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }

    @Test
    void pipelineIntegration_validation() throws Exception {
        WorkflowJob foo = jenkinsRule.jenkins.createProject(WorkflowJob.class, "foo");
        // TODO JobDSL nicely
        CpsFlowDefinition flowDef = new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  codesonar conditions: [cyclomaticComplexity(maxCyclomaticComplexity: 30), " +
                        "redAlerts(alertLimit: 1), warningCountIncreaseNewOnly(percentage: '5.0'), " +
                        "warningCountIncreaseOverall('5.0'), " +
                        "warningCountIncreaseSpecifiedScoreAndHigher(rankOfWarnings: 30, warningPercentage: '5.0'), " +
                        "yellowAlerts(alertLimit: 1)], credentialId: '', " +
                        "hubAddress: '10', projectName: '${JOB_NAME}', protocol: 'http', visibilityFilter: '2'",
                "}"), "\n"), true);
        foo.setDefinition(flowDef);

        WorkflowRun b = foo.scheduleBuild2(0).get();

        boolean valid = false;
        List<String> log = b.getLog(500);
        for (String line : log) {
            if (line.equals(String.format("ERROR: %s", CodeSonarLogger.formatMessage("Error on url: {0}", "http://10/index.xml")))) {
                valid = true;
                break;
            }
        }

        assertThat(valid, is(true));
    }
}
