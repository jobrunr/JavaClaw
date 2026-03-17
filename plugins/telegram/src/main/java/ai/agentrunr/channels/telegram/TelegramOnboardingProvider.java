package ai.agentrunr.channels.telegram;

import ai.agentrunr.configuration.ConfigurationManager;
import ai.agentrunr.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Order(50)
public class TelegramOnboardingProvider implements OnboardingProvider {

    static final String SESSION_TOKEN = "onboarding.telegram.token";
    static final String SESSION_USERNAME = "onboarding.telegram.username";

    @Override
    public boolean isOptional() { return true; }

    @Override
    public String getStepId() { return "telegram"; }

    @Override
    public String getStepTitle() { return "Telegram"; }

    @Override
    public String getTemplatePath() { return "onboarding/steps/telegram"; }

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("telegramUsername", session.getOrDefault(SESSION_USERNAME, ""));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String token = formParams.getOrDefault("telegramBotToken", "").trim();
        String username = normalizeTelegramUsername(formParams.get("telegramUsername"));

        if (username != null) {
            session.put(SESSION_USERNAME, username);
        }

        if (token.isBlank()) {
            return "Enter the Telegram bot token to continue.";
        }
        if (username == null) {
            return "Enter the Telegram username that should be allowed to use the bot.";
        }

        session.put(SESSION_TOKEN, token);
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws IOException {
        String token = (String) session.get(SESSION_TOKEN);
        String username = (String) session.get(SESSION_USERNAME);
        if (token != null && username != null) {
            configurationManager.updateProperties(Map.of(
                    "agent.channels.telegram.token", token,
                    "agent.channels.telegram.username", username
            ));
        }
    }

    private static String normalizeTelegramUsername(String telegramUsername) {
        if (telegramUsername == null) return null;
        String normalized = telegramUsername.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? null : normalized;
    }
}
