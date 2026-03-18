package ai.agentrunr.providers.anthropic;

import com.anthropic.backends.AnthropicBackend;
import com.anthropic.backends.Backend;
import com.anthropic.core.http.HttpRequest;
import com.anthropic.core.http.HttpRequestBody;
import com.anthropic.core.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class AnthropicClaudeCodeBackend implements Backend {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClaudeCodeBackend.class);

    private static final String ANTHROPIC_BETA = "claude-code-20250219,oauth-2025-04-20,interleaved-thinking-2025-05-14";
    private static final String SYSTEM_PREFIX = "You are Claude Code, Anthropic's official CLI for Claude.";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AnthropicBackend delegate;

    AnthropicClaudeCodeBackend() {
        this.delegate = AnthropicBackend.builder().authToken("placeholder").build();
    }

    @Override
    public String baseUrl() {
        return delegate.baseUrl();
    }

    @Override
    public HttpRequest prepareRequest(HttpRequest request) {
        HttpRequest prepared = delegate.prepareRequest(request);
        HttpRequestBody originalBody = prepared.body();
        if (originalBody == null) {
            return prepared;
        }
        return prepared.toBuilder()
                .body(new ClaudeCodeRequestBody(originalBody))
                .build();
    }

    @Override
    public HttpRequest authorizeRequest(HttpRequest request) {
        String token = AnthropicClaudeCodeOAuthTokenExtractor
                .getToken()
                .orElseThrow(() -> new IllegalStateException("No valid Claude Code OAuth token found. Run 'claude auth login' to authenticate."));

        return request.toBuilder()
                .removeHeaders("x-api-key")
                .putHeader("Authorization", "Bearer " + token)
                .putHeader("anthropic-beta", ANTHROPIC_BETA)
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

    /**
     * Wraps the original request body and injects the required Claude Code system
     * prompt prefix in array format with cache_control.
     */
    private static class ClaudeCodeRequestBody implements HttpRequestBody {

        private final HttpRequestBody delegate;
        private byte[] modifiedBytes;

        ClaudeCodeRequestBody(HttpRequestBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public void writeTo(OutputStream os) {
            try {
                if (modifiedBytes == null) {
                    modifiedBytes = buildModifiedBody();
                }
                os.write(modifiedBytes);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write modified request body", e);
            }
        }

        @Override
        public String contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            if (modifiedBytes == null) {
                try {
                    modifiedBytes = buildModifiedBody();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to build modified request body", e);
                }
            }
            return modifiedBytes.length;
        }

        @Override
        public boolean repeatable() {
            return true;
        }

        @Override
        public void close() {
            delegate.close();
        }

        private byte[] buildModifiedBody() throws IOException {
            var baos = new ByteArrayOutputStream();
            delegate.writeTo(baos);
            byte[] original = baos.toByteArray();

            try {
                ObjectNode root = (ObjectNode) objectMapper.readTree(original);
                injectSystemPrefix(root);
                return objectMapper.writeValueAsBytes(root);
            } catch (Exception e) {
                log.warn("Failed to inject Claude Code system prefix: {}", e.getMessage());
                return original;
            }
        }

        private static void injectSystemPrefix(ObjectNode root) {
            ArrayNode systemArray = objectMapper.createArrayNode();

            // First block: the required Claude Code prefix with cache_control
            ObjectNode prefixBlock = objectMapper.createObjectNode();
            prefixBlock.put("type", "text");
            prefixBlock.put("text", SYSTEM_PREFIX);
            ObjectNode cacheControl = objectMapper.createObjectNode();
            cacheControl.put("type", "ephemeral");
            prefixBlock.set("cache_control", cacheControl);
            systemArray.add(prefixBlock);

            // Second block: the actual system prompt from the application
            JsonNode existing = root.get("system");
            if (existing != null) {
                if (existing.isTextual()) {
                    ObjectNode textBlock = objectMapper.createObjectNode();
                    textBlock.put("type", "text");
                    textBlock.put("text", existing.asText());
                    systemArray.add(textBlock);
                } else if (existing.isArray()) {
                    for (JsonNode block : existing) {
                        systemArray.add(block);
                    }
                }
            }

            root.set("system", systemArray);
        }
    }
}
