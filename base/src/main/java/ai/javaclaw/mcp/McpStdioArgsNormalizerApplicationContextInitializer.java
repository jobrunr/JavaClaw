package ai.javaclaw.mcp;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Ordered;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes stdio MCP connection properties by ensuring an `args` list exists
 * when a stdio connection is declared. Implemented as an
 * ApplicationContextInitializer to avoid deprecated APIs.
 */
public class McpStdioArgsNormalizerApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private static final String PREFIX = "spring.ai.mcp.client.stdio.connections.";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        Map<String, Object> additions = new HashMap<>();

        Set<String> names = new java.util.HashSet<>();
        for (org.springframework.core.env.PropertySource<?> ps : environment.getPropertySources()) {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                for (String propName : eps.getPropertyNames()) {
                    if (propName.startsWith(PREFIX)) {
                        String remainder = propName.substring(PREFIX.length());
                        int idx = remainder.indexOf('.');
                        String name = idx > 0 ? remainder.substring(0, idx) : remainder;
                        names.add(name);
                    }
                }
            }
        }

        for (String name : names) {
            String cmdKey = PREFIX + name + ".command";
            String argsKey = PREFIX + name + ".args";
            if (environment.containsProperty(cmdKey) && !environment.containsProperty(argsKey)) {
                additions.put(argsKey, java.util.List.of());
            }
        }

        if (!additions.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("mcpStdioArgsNormalizer", additions));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
