package ai.agentrunr.onboarding.steps;

import ai.agentrunr.configuration.ConfigurationManager;
import ai.agentrunr.onboarding.OnboardingProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(50)
public class S5_McpStep implements OnboardingProvider {

    static final String SESSION_MCP_SERVERS = "onboarding.mcp.servers";

    @Override
    public String getStepId() {return "mcp";}

    @Override
    public String getStepTitle() {return "MCP Servers";}

    @Override
    public String getTemplatePath() {return "onboarding/steps/S5-mcp";}

    @Override
    public boolean isOptional() {return true;}

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        model.put("servers", getServers(session));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String action = formParams.getOrDefault("action", "continue");
        List<Map<String, Object>> servers = getServers(session);

        if ("remove".equals(action)) {
            int index = Integer.parseInt(formParams.getOrDefault("serverIndex", "-1"));
            if (index >= 0 && index < servers.size()) {
                servers.remove(index);
            }
            session.put(SESSION_MCP_SERVERS, servers);
            return ""; // stay on step without showing an error
        }

        if ("add".equals(action)) {
            String name = formParams.getOrDefault("serverName", "").trim();
            String type = formParams.getOrDefault("serverType", "streamable-http").trim();

            if (name.isBlank()) return "Server name is required.";
            if (!name.matches("[a-zA-Z0-9_-]+"))
                return "Server name may only contain letters, numbers, hyphens and underscores.";
            if (servers.stream().anyMatch(s -> name.equals(s.get("name"))))
                return "A server named '" + name + "' already exists.";

            Map<String, Object> server = new LinkedHashMap<>();
            server.put("name", name);
            server.put("type", type);

            if ("streamable-http".equals(type)) {
                String url = formParams.getOrDefault("serverUrl", "").trim();
                if (url.isBlank()) return "URL is required for streamable-http.";
                server.put("url", url);
                server.put("headers", parseColonSeparated(formParams.getOrDefault("serverHeaders", "")));
            } else if ("stdio".equals(type)) {
                String command = formParams.getOrDefault("serverCommand", "").trim();
                if (command.isBlank()) return "Command is required for stdio.";
                server.put("command", command);
                server.put("args", parseLines(formParams.getOrDefault("serverArgs", "")));
                server.put("env", parseEqualsSeparated(formParams.getOrDefault("serverEnv", "")));
            } else {
                return "Unknown server type: " + type;
            }

            servers.add(server);
            session.put(SESSION_MCP_SERVERS, servers);
            return ""; // stay on step to show the newly added server
        }

        // action = "continue" — advance to next step
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws IOException {
        List<Map<String, Object>> servers = getServers(session);
        if (servers.isEmpty()) return;

        Map<String, Object> props = new LinkedHashMap<>();
        for (Map<String, Object> server : servers) {
            String name = (String) server.get("name");
            String type = (String) server.get("type");

            if ("streamable-http".equals(type)) {
                props.put("spring.ai.mcp.client.streamable-http.connections." + name + ".url", server.get("url"));
                Map<String, String> headers = (Map<String, String>) server.get("headers");
                headers.forEach((k, v) ->
                        props.put("spring.ai.mcp.client.streamable-http.connections." + name + ".headers." + k, v));
            } else if ("stdio".equals(type)) {
                props.put("spring.ai.mcp.client.stdio.connections." + name + ".command", server.get("command"));
                List<String> args = (List<String>) server.get("args");
                if (!args.isEmpty()) {
                    props.put("spring.ai.mcp.client.stdio.connections." + name + ".args", args);
                }
                Map<String, String> env = (Map<String, String>) server.get("env");
                env.forEach((k, v) ->
                        props.put("spring.ai.mcp.client.stdio.connections." + name + ".env." + k, v));
            }
        }
        configurationManager.updateProperties(props);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getServers(Map<String, Object> session) {
        Object existing = session.get(SESSION_MCP_SERVERS);
        if (existing instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) existing);
        }
        return new ArrayList<>();
    }

    /**
     * Parses {@code Key: Value} lines into a map.
     */
    private Map<String, String> parseColonSeparated(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            int sep = line.indexOf(':');
            if (sep > 0) {
                result.put(line.substring(0, sep).trim(), line.substring(sep + 1).trim());
            }
        }
        return result;
    }

    /**
     * Parses {@code KEY=VALUE} lines into a map.
     */
    private Map<String, String> parseEqualsSeparated(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            int sep = line.indexOf('=');
            if (sep > 0) {
                result.put(line.substring(0, sep).trim(), line.substring(sep + 1).trim());
            }
        }
        return result;
    }

    /**
     * Parses one value per line into a list, ignoring blanks.
     */
    private List<String> parseLines(String text) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
