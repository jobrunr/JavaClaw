package ai.agentrunr.tools.brave;

import ai.agentrunr.configuration.ConfigurationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ai.agentrunr.tools.brave.BraveOnboardingProvider.AGENT_BROWSER_BRAVE_API_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BraveOnboardingProviderTest {

    @Mock
    Environment environment;

    @Mock
    ConfigurationManager configurationManager;

    @Test
    void stepMetadataIsCorrect() {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);

        assertThat(provider.getStepId()).isEqualTo("brave");
        assertThat(provider.getStepTitle()).isEqualTo("Brave Search");
        assertThat(provider.getTemplatePath()).isEqualTo("onboarding/steps/brave");
        assertThat(provider.isOptional()).isTrue();
    }

    @Test
    void processStepStoresApiKeyInSession() {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        String processStepResult = provider.processStep(Map.of("braveApiKey", "test-key-123"), session);

        assertThat(processStepResult).isNull();
        assertThat(session).containsEntry(BraveOnboardingProvider.SESSION_API_KEY, "test-key-123");
    }

    @Test
    void processStepTrimsApiKey() {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();

        provider.processStep(Map.of("braveApiKey", "  trimmed-key  "), session);

        assertThat(session).containsEntry(BraveOnboardingProvider.SESSION_API_KEY, "trimmed-key");
    }

    @Test
    void processStepDoesNotOverwriteSessionWhenKeyIsBlank() {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);
        Map<String, Object> session = new HashMap<>();
        session.put(BraveOnboardingProvider.SESSION_API_KEY, "existing-key");

        provider.processStep(Map.of("braveApiKey", ""), session);

        assertThat(session).containsEntry(BraveOnboardingProvider.SESSION_API_KEY, "existing-key");
    }

    @Test
    void prepareModelUsesFallbackFromEnvironment() {
        Environment env = environment;
        when(env.getProperty(AGENT_BROWSER_BRAVE_API_KEY, "")).thenReturn("env-key");
        BraveOnboardingProvider provider = new BraveOnboardingProvider(env);
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(Map.of(), model);

        assertThat(model).containsEntry("braveApiKey", "env-key");
    }

    @Test
    void prepareModelPrefersSessionOverEnvironment() {
        Environment env = environment;
        when(env.getProperty(AGENT_BROWSER_BRAVE_API_KEY, "")).thenReturn("env-key");
        BraveOnboardingProvider provider = new BraveOnboardingProvider(env);
        Map<String, Object> session = Map.of(BraveOnboardingProvider.SESSION_API_KEY, "session-key");
        Map<String, Object> model = new HashMap<>();

        provider.prepareModel(session, model);

        assertThat(model).containsEntry("braveApiKey", "session-key");
    }

    @Test
    void saveConfigurationWritesApiKey() throws IOException {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);
        Map<String, Object> session = Map.of(BraveOnboardingProvider.SESSION_API_KEY, "my-api-key");

        provider.saveConfiguration(session, configurationManager);

        verify(configurationManager).updateProperties(Map.of(AGENT_BROWSER_BRAVE_API_KEY, "my-api-key"));
    }

    @Test
    void saveConfigurationDoesNothingWhenKeyAbsentFromSession() throws IOException {
        BraveOnboardingProvider provider = new BraveOnboardingProvider(environment);

        provider.saveConfiguration(Map.of(), configurationManager);

        verifyNoInteractions(configurationManager);
    }
}