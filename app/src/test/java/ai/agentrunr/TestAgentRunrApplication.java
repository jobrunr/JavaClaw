package ai.agentrunr;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestAgentRunrApplication {

    public static void main(String[] args) {
        SpringApplication.from(AgentRunrApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
