package ai.agentrunr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class AgentRunrApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentRunrApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AgentRunrApplication.class);
        app.setDefaultProperties(Map.of("spring.ai.mcp.client.enabled", "false"));
        app.run(args);
    }

    @Component
    static public class AgentRunrApplicationInitializer implements ApplicationRunner {


        private final Environment environment;

        public AgentRunrApplicationInitializer(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void run(ApplicationArguments args) throws Exception {
            String isConfigured = environment.getProperty("agent.onboarding.completed");
            if (Boolean.parseBoolean(isConfigured)) {
                log.info("AgentRunr is running and waiting for your commands!");
            } else {
                log.info("AgentRunr is waiting to be configured! Navigate to http://localhost:{}/onboarding to start the onboarding wizard", environment.getProperty("local.server.port"));
            }
        }
    }
}
