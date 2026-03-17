package ai.agentrunr;

import ai.agentrunr.providers.anthropic.AnthropicClaudeCodeOAuthTokenExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ai.agentrunr.providers.anthropic.AnthropticClaudeCodeConfiguration.CLAUDE_CODE_OATH_TOKEN_PLACEHOLDER;

public enum SupportedProvider {

    OLLAMA("ollama", "Ollama", "Local-first setup. No API key required.", false, "qwen3.5:27b"),
    OPENAI("openai", "OpenAI", "Uses OpenAI API key for ChatGPT as an agent.", true, "gpt-5.4"),
    ANTHROPIC("anthropic", "Anthropic", "Uses Claude Code or Anthropic credentials for Claude-based chat", true, "claude-sonnet-4-6") {
        @Override
        public Optional<SystemWideToken> systemWideToken() {
            Optional<String> token = AnthropicClaudeCodeOAuthTokenExtractor.getToken();
            if (token.isEmpty()) return Optional.empty();

            return Optional.of(new SystemWideToken("Claude Code", CLAUDE_CODE_OATH_TOKEN_PLACEHOLDER));
        }
    };

    private final String id;
    private final String label;
    private final String slogan;
    private final boolean requiresApiKey;
    private final String defaultModel;

    SupportedProvider(String id, String label, String slogan, boolean requiresApiKey, String defaultModel) {
        this.id = id;
        this.label = label;
        this.slogan = slogan;
        this.requiresApiKey = requiresApiKey;
        this.defaultModel = defaultModel;
    }

    public String id() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String label() {
        return label;
    }

    public String slogan() {
        return slogan;
    }

    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public Optional<SystemWideToken> systemWideToken() {
        return Optional.empty();
    }

    public String createPropertyKey(String propertySuffix) {
        return "spring.ai." + id() + "." + propertySuffix;
    }

    public void saveProperty(Map<String, Object> properties, String propertySuffix, String value) {
        if (value == null || value.isBlank()) return;
        properties.put(createPropertyKey(propertySuffix), value);
    }

    public static List<SupportedProvider> supportedAgents() {
        return Arrays.asList(values());
    }

    public static Optional<SupportedProvider> from(String value) {
        return Arrays.stream(values())
                .filter(provider -> provider.id.equalsIgnoreCase(value))
                .findFirst();
    }

    public record SystemWideToken(String name, String token) {}
}