package ai.agentrunr.tools;

import java.io.File;
import java.time.Instant;

public class AgentEnvironment {

    public static final String ENVIRONMENT_INFO_KEY = "ENVIRONMENT_INFO";

    private static final AgentEnvironment INSTANCE;

    static {
        INSTANCE = new AgentEnvironment();
    }

    private AgentEnvironment() {}

    public static AgentEnvironment info() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        String workingDirectory = System.getProperty("user.dir");
        boolean isGitRepo = new File(workingDirectory, ".git").exists();
        String platform = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String timezone = System.getProperty("user.timezone");

        StringBuilder sb = new StringBuilder().append(System.lineSeparator());
        sb.append("Working directory: ").append(workingDirectory).append(System.lineSeparator());
        sb.append("Is directory a git repo: ").append(isGitRepo ? "Yes" : "No").append(System.lineSeparator());
        sb.append("Platform: ").append(platform).append(System.lineSeparator());
        sb.append("OS Version: ").append(osVersion).append(System.lineSeparator());
        sb.append("Timezone: ").append(timezone).append(System.lineSeparator());
        sb.append("Current time: ").append(Instant.now()).append(System.lineSeparator());

        return sb.toString();
    }
}
