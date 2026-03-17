package ai.agentrunr.providers.anthropic;

import com.anthropic.backends.AnthropicBackend;
import com.anthropic.backends.Backend;
import com.anthropic.core.http.HttpRequest;
import com.anthropic.core.http.HttpResponse;

class AnthropicClaudeCodeBackend implements Backend {


    private static final String OAUTH_BETA = "oauth-2025-04-20";

    private final AnthropicBackend delegate;


    AnthropicClaudeCodeBackend() {
        // Build delegate with a placeholder token so prepareRequest() adds all standard
        // Anthropic headers (anthropic-version, etc.); we overwrite the auth header ourselves.
        this.delegate = AnthropicBackend.builder().authToken("placeholder").build();
    }

    @Override
    public String baseUrl() {
        return delegate.baseUrl();
    }

    @Override
    public HttpRequest prepareRequest(HttpRequest request) {
        return delegate.prepareRequest(request);
    }

    @Override
    public HttpRequest authorizeRequest(HttpRequest request) {
        String token = AnthropicClaudeCodeOAuthTokenExtractor
                .getToken()
                .orElseThrow(() -> new IllegalStateException("No valid Claude Code OAuth token found. Run 'claude auth login' to authenticate."));

        HttpRequest prepared = delegate.authorizeRequest(request);
        return prepared.toBuilder()
                .replaceHeaders("Authorization", "Bearer " + token)
                .putHeader("anthropic-beta", OAUTH_BETA)
                .build();
    }

    @Override
    public HttpResponse prepareResponse(HttpResponse response) {
        return delegate.prepareResponse(response);
    }

    @Override
    public void close() {
        delegate.close();
    }


}
