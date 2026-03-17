package ai.agentrunr.onboarding.api;

import ai.agentrunr.SupportedProvider;
import ai.agentrunr.configuration.ConfigurationManager;
import ai.agentrunr.onboarding.OnboardingProvider;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OnboardingController {

    private static final String COMPLETE_STEP_ID = "complete";
    private static final String ONBOARDING_TEMPLATE = "onboarding/index";

    private final Environment environment;
    private final ConfigurationManager configurationManager;
    private final List<OnboardingProvider> steps;

    public OnboardingController(Environment environment, ConfigurationManager configurationManager, List<OnboardingProvider> steps) {
        this.environment = environment;
        this.configurationManager = configurationManager;
        this.steps = steps;
    }

    @GetMapping({"/onboarding", "/onboarding/"})
    public String onboarding() {
        return "redirect:/onboarding/" + steps.getFirst().getStepId();
    }

    @GetMapping("/onboarding/{stepId}")
    public String getStep(@PathVariable String stepId, HttpSession session, Model model) {
        OnboardingProvider onboardingProvider = findProvider(stepId);
        if (onboardingProvider == null) {
            return "redirect:/onboarding/" + steps.getFirst().getStepId();
        }

        Map<String, Object> sessionMap = sessionToMap(session);
        Map<String, Object> stepModel = new HashMap<>(model.asMap());
        onboardingProvider.prepareModel(sessionMap, stepModel);
        stepModel.forEach(model::addAttribute);

        int idx = stepIndex(stepId);
        model.addAttribute("currentStep", onboardingProvider.getStepTitle());
        model.addAttribute("currentStepNumber", idx + 1);
        model.addAttribute("totalSteps", steps.size());
        model.addAttribute("steps", steps.stream().map(p -> Map.of("title", p.getStepTitle(), "optional", p.isOptional())).toList());
        model.addAttribute("previousStepUrl", idx > 0 ? "/onboarding/" + steps.get(idx - 1).getStepId() : null);
        model.addAttribute("nextStepUrl", idx < steps.size() - 1 ? "/onboarding/" + steps.get(idx + 1).getStepId() : null);
        model.addAttribute("isOptional", onboardingProvider.isOptional());
        model.addAttribute("stepTemplate", onboardingProvider.getTemplatePath());
        return ONBOARDING_TEMPLATE;
    }

    @PostMapping("/onboarding/{stepId}")
    public String postStep(@PathVariable String stepId, @RequestParam Map<String, String> formParams, HttpSession session, RedirectAttributes redirectAttrs) {
        OnboardingProvider provider = findProvider(stepId);
        if (provider == null) {
            return "redirect:/onboarding/" + steps.getFirst().getStepId();
        }

        Map<String, Object> sessionMap = sessionToMap(session);
        String error = provider.processStep(formParams, sessionMap);
        syncSessionFromMap(session, sessionMap);

        if (error != null) {
            redirectAttrs.addFlashAttribute("error", error);
            return "redirect:/onboarding/" + stepId;
        }

        String nextId = nextStepId(stepId);
        if (COMPLETE_STEP_ID.equals(nextId)) {
            Map<String, Object> finalSession = sessionToMap(session);
            // Pass providerLabel to complete page before clearing session
            String providerId = (String) finalSession.getOrDefault("onboarding.provider", "");
            SupportedProvider.from(providerId).map(SupportedProvider::label)
                    .ifPresent(label -> redirectAttrs.addFlashAttribute("providerLabel", label));
            try {
                for (OnboardingProvider p : steps) {
                    p.saveConfiguration(finalSession, configurationManager);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to persist onboarding configuration", e);
            }
            clearOnboardingSession(session);
        }

        return "redirect:/onboarding/" + (nextId != null ? nextId : COMPLETE_STEP_ID);
    }

    private OnboardingProvider findProvider(String stepId) {
        return steps.stream()
                .filter(p -> p.getStepId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    private int stepIndex(String stepId) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStepId().equals(stepId)) return i;
        }
        return 0;
    }

    private String nextStepId(String stepId) {
        int idx = stepIndex(stepId);
        return idx < steps.size() - 1 ? steps.get(idx + 1).getStepId() : null;
    }

    private Map<String, Object> sessionToMap(HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        Collections.list(session.getAttributeNames()).forEach(name -> map.put(name, session.getAttribute(name)));
        return map;
    }

    private void syncSessionFromMap(HttpSession session, Map<String, Object> map) {
        map.forEach(session::setAttribute);
    }

    private void clearOnboardingSession(HttpSession session) {
        Collections.list(session.getAttributeNames()).stream()
                .filter(name -> name.startsWith("onboarding."))
                .toList()
                .forEach(session::removeAttribute);
    }
}