package ai.agentrunr.onboarding.steps;

import ai.agentrunr.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(10)
public class S1_WelcomeStep implements OnboardingProvider {

    @Override
    public String getStepId() {return "welcome";}

    @Override
    public String getStepTitle() {return "Welcome";}

    @Override
    public String getTemplatePath() {return "onboarding/steps/S1-welcome";}

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        return null;
    }
}
