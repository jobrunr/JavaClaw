package ai.javaclaw.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

public class McpStdioArgsNormalizerApplicationContextRunnerTest {

    /**
     * Verifies that {@code StdioTransportAutoConfiguration} does not throw
     * {@code "The args can not be null"} when a stdio MCP connection is declared
     * without an explicit args list — the normalizer must inject an empty list before
     * autoconfiguration runs.
     */
    @Test
    void contextDoesNotFailWhenArgsMissingAndNormalizerPresent() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withInitializer(new McpStdioArgsNormalizerApplicationContextInitializer())
                .withPropertyValues("spring.ai.mcp.client.stdio.connections.foo.command=dummy-cmd")
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.ai.mcp.client.common.autoconfigure.StdioTransportAutoConfiguration.class
                ));

        runner.run(context -> {
            assertNull(context.getStartupFailure(), "expected context to start without startup failure");
        });
    }
}
