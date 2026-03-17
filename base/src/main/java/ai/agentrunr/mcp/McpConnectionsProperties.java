package ai.agentrunr.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("spring.ai.mcp.client.streamable-http")
public record McpConnectionsProperties(Map<String, Connection> connections) {

    public McpConnectionsProperties {
        connections = connections != null ? connections : Map.of();
    }

    public record Connection(String url, Map<String, String> headers) {

        public Connection {
            url = url != null ? url : "";
            headers = headers != null ? headers : Map.of();
        }
    }
}
