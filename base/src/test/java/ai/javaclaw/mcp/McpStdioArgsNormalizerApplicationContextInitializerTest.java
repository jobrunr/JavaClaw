package ai.javaclaw.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class McpStdioArgsNormalizerApplicationContextInitializerTest {

    @Test
    void addsEmptyArgsWhenCommandPresentAndArgsMissing() {
        StandardEnvironment env = new StandardEnvironment();
        MapPropertySource src = new MapPropertySource("test", Map.of(
                "spring.ai.mcp.client.stdio.connections.foo.command", "dummy-cmd"
        ));
        env.getPropertySources().addFirst(src);

        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setEnvironment(env);

        McpStdioArgsNormalizerApplicationContextInitializer init = new McpStdioArgsNormalizerApplicationContextInitializer();
        init.initialize(ctx);

        String argsKey = "spring.ai.mcp.client.stdio.connections.foo.args";

        assertNotNull(env.getPropertySources().get("mcpStdioArgsNormalizer"), "expected normalizer property source to exist");

        Object raw = env.getPropertySources().get("mcpStdioArgsNormalizer").getProperty(argsKey);
        assertNotNull(raw, "expected args property to be present in the normalizer property source");
        assertTrue(raw instanceof List, "expected raw args value to be a List");
        assertTrue(((List<?>) raw).isEmpty(), "expected args list to be empty");
    }

    @Test
    void doesNotOverwriteExistingArgs() {
        StandardEnvironment env = new StandardEnvironment();
        MapPropertySource src = new MapPropertySource("test", Map.of(
                "spring.ai.mcp.client.stdio.connections.bar.command", "cmd",
                "spring.ai.mcp.client.stdio.connections.bar.args", List.of("a")
        ));
        env.getPropertySources().addFirst(src);

        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setEnvironment(env);

        McpStdioArgsNormalizerApplicationContextInitializer init = new McpStdioArgsNormalizerApplicationContextInitializer();
        init.initialize(ctx);

        assertNull(env.getPropertySources().get("mcpStdioArgsNormalizer"), "expected normalizer property source to be absent when no additions were required");

        Object raw = env.getPropertySources().get("test").getProperty("spring.ai.mcp.client.stdio.connections.bar.args");
        assertNotNull(raw);
        assertTrue(raw instanceof List);
        assertEquals(1, ((List<?>) raw).size());
    }
}
