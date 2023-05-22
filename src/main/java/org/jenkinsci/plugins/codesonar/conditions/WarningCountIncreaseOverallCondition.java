package org.jenkinsci.plugins.codesonar.conditions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.api.CodeSonarHubAnalysisDataLoader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Throwables;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

public class WarningCountIncreaseOverallCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountIncreaseOverallCondition.class.getName());

    private static final String NAME = "Warning count increase: overall";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "threshold={0,number,0.00}%, increase={1,number,0.00}% (count: current={2,number,0}, previous={3,number,0})";
    private String percentage = String.valueOf(5.0f);
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseOverallCondition(String percentage) {
        this.percentage = percentage;
    }

    /**
     * @return the percentage
     */
    public String getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarHubAnalysisDataLoader previous, String visibilityFilter, String newVisibilityFilter, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }        

        if (previous == null) {
            registerResult(csLogger, PREVIOUS_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        Long previousCount = null;
        try {
            previousCount = previous.getNumberOfActiveWarnings();
        } catch (IOException e) {
            final String applicationMsg = "Error calling number of active warnings on HUB API for previous analysis.";
            LOGGER.log(Level.WARNING, applicationMsg);
            registerResult(csLogger, applicationMsg);
            csLogger.writeInfo("Exception: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
            return Result.FAILURE;
        }
        
        Long currentCount = null;
        try {
            currentCount = current.getNumberOfActiveWarnings();
        } catch (IOException e) {
            final String applicationMsg = "Error calling number of active warnings on HUB API for current analysis.";
            LOGGER.log(Level.WARNING, applicationMsg);
            registerResult(csLogger, applicationMsg);
            csLogger.writeInfo("Exception: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
            return Result.FAILURE;
        }
        
        // Going to produce build failures in the case of missing necessary information
        if(previousCount == null) {
            LOGGER.log(Level.SEVERE, "\"previousAnalysisActiveWarningsCount\" not available.");
            registerResult(csLogger, DATA_LOADER_EMPTY_RESPONSE);
            return Result.FAILURE;
        }
        if(currentCount == null) {
            LOGGER.log(Level.SEVERE, "\"currentAnalysisActiveWarningsCount\" not available.");
            registerResult(csLogger, DATA_LOADER_EMPTY_RESPONSE);
            return Result.FAILURE;
        }        
        
        long diff = currentCount.longValue() - previousCount.longValue();
        float thresholdPercentage = Float.parseFloat(percentage);
        
        float result;
        //If there are no warnings, redefine percentage of new warnings
        if(previousCount.longValue() == 0) {
            result = diff > 0 ? 100f : 0f;
            LOGGER.log(Level.INFO, "no active warnings found, forcing warning percentage to {0,number,0.00}%", result);
        } else {
            result = (((float) diff) / previousCount.longValue()) * 100;
            LOGGER.log(Level.INFO, "warnings increment percentage = {0,number,0.00}%", result);
        }
        
        if (result > thresholdPercentage) {
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, thresholdPercentage, result, diff, previousCount);
            return Result.fromString(warrantedResult);
        }
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, thresholdPercentage, result, diff, previousCount);
        return Result.SUCCESS;
    }

    @Symbol("warningCountIncreaseOverall")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseOverallCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckPercentage(@QueryParameter("percentage") String percentage) {
            if (StringUtils.isBlank(percentage)) {
                return FormValidation.error("Cannot be empty");
            }

            try {
                float v = Float.parseFloat(percentage);

                if(v < 0) {
                    return FormValidation.error("The provided value must be zero or greater");
                }
            } catch (NumberFormatException numberFormatException) {
                return FormValidation.error("Not a valid decimal number");
            }

            return FormValidation.ok();
        }

    }
    
}
