package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.api.CodeSonarAlertCounter;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarAlertData;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class AlertsService {
    private static final Logger LOGGER = Logger.getLogger(AlertsService.class.getName());
    
    final private HttpService httpService;

    public AlertsService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    public CodeSonarAlertCounter getAlertCounter(URI baseHubUri, long analysisId) throws CodeSonarPluginException {
        CodeSonarAlertCounter counter = new CodeSonarAlertCounter();
        boolean foundExpectedResponse = false;
        int unexpectedErrorCounter = 0;
        //Loop through all possible kind values, currently from 0 to 26. Upper bound 50 is for future alert kinds.
        for(long kind = 0; kind < 50; kind++) {
            //Build char_table request URL
            URIBuilder uriBuilder = new URIBuilder(baseHubUri);
            uriBuilder.setPath(String.format("/analysis/%d-alert%d.json", analysisId, kind));
            
            URI requestUri = null;
            try {
                requestUri = uriBuilder.build();
            } catch (URISyntaxException e) {
                throw new CodeSonarPluginException("Request URI for Alerts API contains a syntax error. %nException: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
            }
            String requestUriString = requestUri.toASCIIString();
            
            LOGGER.log(Level.INFO, "Querying alerts with kind={0}", kind);
            HttpServiceResponse response = null;
            try {
                response = httpService.getResponseFromUrl(requestUriString);
            
                if(response.getStatusCode() == 200) {
                    foundExpectedResponse = true;
                } else if(response.getStatusCode() == 404) {
                    /*
                     * A 404 response is expected to be returned anytime the request specifies an alert kind that has no
                     * occurrences on the specified analysis
                     */
                    LOGGER.log(Level.INFO, "Skipping current alert kind");
                    foundExpectedResponse = true;
                    continue;
                } else if(response.getStatusCode() == 500) {
                    /*
                     * A 500 response is expected to be returned anytime the request specifies an alert kind that is out
                     * of range with respect to all currently supported alert kinds.
                     * This makes status 500 the right one to establish when to stop looping through kinds.
                     */
                    LOGGER.log(Level.INFO, MessageFormat.format("Stop looping through alert kinds at kind id {0}", kind));
                    break;
                } else {
                    /*
                     * An unexpected server-side error prevented the request from being satisfied, try skipping current
                     * alert kind only if other successful request have already been successfully satisfied.
                     */
                    unexpectedErrorCounter++;
                    if(foundExpectedResponse) {
                        if(unexpectedErrorCounter  > 2) {
                            throw new CodeSonarPluginException("Too many unexpected errors found communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", requestUriString, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
                        } else {
                            LOGGER.log(Level.INFO, "Found an unexpected error in the response, skipping current alert kind");
                            continue;
                        }
                    } else {
                        LOGGER.log(Level.INFO, "Unexpected error in the response without earlier expected ones, stop looping.");
                        throw new CodeSonarPluginException("Unexpected error in the response without earlier expected ones communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", requestUriString, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
                    }
                }
            } catch(CodeSonarPluginException e) {
                /*
                 * An generic error prevented the response from being received, try skipping current alert kind
                 */
                unexpectedErrorCounter++;
                if(unexpectedErrorCounter > 2) {
                    throw new CodeSonarPluginException("Too many exeptions communicating with CodeSonar Hub. %nURI: {0}%nException: {1}%nStack Trace: {2}", requestUriString, e.getMessage(), Throwables.getStackTraceAsString(e));
                } else {
                    LOGGER.log(Level.INFO, "Exception querying the hub, skipping current alert kind", e);
                    continue;
                }
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read unexpected response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", requestUriString, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
    
            Gson gsonDeserializer = new Gson();
            CodeSonarAlertData alertData = null;
            try {
                alertData = gsonDeserializer.fromJson(new InputStreamReader(response.getContentInputStream(), StandardCharsets.UTF_8), CodeSonarAlertData.class);
                LOGGER.log(Level.INFO, "response alertData={0}", alertData.toString());
            } catch(JsonSyntaxException e) {
                LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
                return null;
            }
            
            if(alertData.getUndef_funcs() != null && alertData.getUndef_funcs().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"undef_funcs\" for color {1}", new Object[] {alertData.getUndef_funcs().size(), alertData.getColor()});
                counter.incrementOf(alertData.getColor(), alertData.getUndef_funcs().size());
            }
            if(alertData.getIssues() != null && alertData.getIssues().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"issues\" for color {1}", new Object[] {alertData.getIssues().size(), alertData.getColor()});
                counter.incrementOf(alertData.getColor(), alertData.getIssues().size());
            }
            if(alertData.getFacts() != null && alertData.getFacts().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"facts\" for color {1}", new Object[] {alertData.getFacts().size(), alertData.getColor()});
                counter.incrementOf(alertData.getColor(), alertData.getFacts().size());
            }
        }
        
        return counter;
    }

}
