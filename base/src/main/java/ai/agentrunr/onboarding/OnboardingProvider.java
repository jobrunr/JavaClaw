package ai.agentrunr.onboarding;

import ai.agentrunr.configuration.ConfigurationManager;

import java.util.Map;

/**
 * Represents a single step in the onboarding flow.
 * <p>
 * Implementations live in any module (e.g. {@code app}, {@code channels:telegram}) and are
 * auto-discovered by Spring's component scan. Use {@code @Order} to control position in the flow.
 * </p>
 */
public interface OnboardingProvider {

    /** Unique URL-safe identifier used in {@code /onboarding/{stepId}}. */
    String getStepId();

    /** Human-readable label shown in the sidebar step list. */
    String getStepTitle();

    /** Classpath template path (without suffix), e.g. {@code "onboarding/steps/1-welcome"}. */
    String getTemplatePath();

    /**
     * Populates the Pebble model for the GET render of this step.
     *
     * @param session a live snapshot of the current onboarding session attributes
     * @param model   map to add template variables into
     */
    void prepareModel(Map<String, Object> session, Map<String, Object> model);

    /**
     * Validates and processes the submitted form for this step.
     *
     * @param formParams submitted HTTP request parameters
     * @param session    a live snapshot of the session — mutate to persist values
     * @return {@code null} on success; a non-null error message to flash back to the user on failure
     */
    String processStep(Map<String, String> formParams, Map<String, Object> session);

    /**
     * Whether this step can be skipped. Defaults to {@code false}.
     * When {@code true}, the controller passes {@code isOptional=true} to the model so the
     * template can render a Skip button.
     */
    default boolean isOptional() {
        return false;
    }

    /**
     * Persists this step's configuration to {@link ConfigurationManager}.
     * Called once for all providers when the last content step completes successfully.
     * Default implementation is a no-op.
     */
    default void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws java.io.IOException {
    }
}
