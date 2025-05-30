package org.jenkinsci.plugins.codesonar.integration.conditions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.codesonar.AnalysisServiceFactory;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.AuthenticationService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 *
 * @author Andrius
 */
@WithJenkins
abstract class ConditionIntegrationTestBase {

    protected JenkinsRule jenkinsRule;

    protected IAnalysisService mockedAnalysisService;
    protected MetricsService mockedMetricsService;
    protected ProceduresService mockedProceduresService;
    protected AuthenticationService mockedAuthenticationService;

    protected HttpService mockedHttpService;

    protected AnalysisServiceFactory mockedAnalysisServiceFactory;

    protected final URI VALID_HUB_ADDRESS = URI.create("10.10.10.10");
    protected final String VALID_PROJECT_NAME = "projectName";
    protected final String VALID_CODESONAR_PROJECT_FILE = "projectName.prj";

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkinsRule = rule;
        mockedAnalysisService = mock(AnalysisService.class);
        mockedMetricsService = mock(MetricsService.class);
        mockedProceduresService = mock(ProceduresService.class);
        mockedAuthenticationService = mock(AuthenticationService.class);
        mockedAnalysisServiceFactory = mock(AnalysisServiceFactory.class);
        mockedHttpService = mock(HttpService.class);

        final URI VALID_ANALYSIS_URL = URI.create("http://10.10.1.102/VALID_ANALYSIS_URL");
        final URI BASE_HUB_URI = URI.create("http://10.10.1.102");
        final long ANALYSIS_ID = 15;
        final Analysis VALID_ANALYSIS_ACTIVE_WARNINGS = new Analysis();
        VALID_ANALYSIS_ACTIVE_WARNINGS.setAnalysisId("10");

        final List<Warning> ACTIVE_WARNINGS = new ArrayList<>();
        Warning warning = new Warning();
        warning.setRank(25);
        warning.setScore(25);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        warning = new Warning();
        warning.setRank(35);
        warning.setScore(35);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        VALID_ANALYSIS_ACTIVE_WARNINGS.setWarnings(ACTIVE_WARNINGS);

        final URI VALID_METRICS_URL = URI.create("http://10.10.1.110/VALID_METRICS_URL");
        final Metrics VALID_METRICS = new Metrics();

        final URI VALID_PROCEDURES_URL = URI.create("http://10.10.1.110/VALID_PROCEDURES_URL");
        final Procedures VALID_PROCEDURES = new Procedures();

        final Analysis VALID_ANALYSIS_NEW_WARNINGS = new Analysis();

        final List<Warning> NEW_WARNINGS = new ArrayList<>();
        warning = new Warning();
        warning.setRank(25);
        NEW_WARNINGS.add(warning);
        NEW_WARNINGS.add(warning);
        NEW_WARNINGS.add(warning);
        VALID_ANALYSIS_NEW_WARNINGS.setWarnings(NEW_WARNINGS);
        when(mockedAnalysisService.getAnalysisUrlFromLogFile(any(List.class)))
                .thenReturn(VALID_ANALYSIS_URL.toString());
        when(mockedAnalysisService.getAnalysisFromUrlWarningsByFilter(BASE_HUB_URI, ANALYSIS_ID))
                .thenReturn(VALID_ANALYSIS_ACTIVE_WARNINGS);
        when(mockedAnalysisService.getLatestAnalysisUrlForAProject(VALID_HUB_ADDRESS, VALID_PROJECT_NAME))
                .thenReturn(VALID_ANALYSIS_URL.toString());

        when(mockedAnalysisServiceFactory.getAnalysisService(any(HttpService.class), any(XmlSerializationService.class))).thenReturn(mockedAnalysisService);

        when(mockedMetricsService.getMetricsUriFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_METRICS_URL);
        when(mockedMetricsService.getMetricsFromUri(VALID_METRICS_URL)).thenReturn(VALID_METRICS);

        when(mockedProceduresService.getProceduresUriFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_PROCEDURES_URL);
        when(mockedProceduresService.getProceduresFromUri(VALID_PROCEDURES_URL)).thenReturn(VALID_PROCEDURES);

        when(mockedAnalysisService.getAnalysisFromUrlWithNewWarnings(BASE_HUB_URI, ANALYSIS_ID))
                .thenReturn(VALID_ANALYSIS_NEW_WARNINGS);

        when(mockedHttpService.getResponseFromUrl(any(URI.class)).readContent())
                .thenReturn("Version: 4.2");
    }
}
