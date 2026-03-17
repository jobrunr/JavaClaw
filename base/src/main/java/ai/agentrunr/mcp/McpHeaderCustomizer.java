package ai.agentrunr.mcp;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Component
@EnableConfigurationProperties(McpConnectionsProperties.class)
public class McpHeaderCustomizer implements McpSyncHttpClientRequestCustomizer {

    private final McpConnectionsProperties properties;

    public McpHeaderCustomizer(McpConnectionsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(HttpRequest.Builder builder, String method, URI endpoint, String body, McpTransportContext context) {
        String endpointStr = endpoint.toString();
        properties.connections().forEach((name, connection) -> {
            if (!connection.url().isBlank() && endpointStr.startsWith(connection.url())) {
                connection.headers().forEach(builder::header);
            }
        });
    }
}
