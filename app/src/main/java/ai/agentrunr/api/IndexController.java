package ai.agentrunr.api;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final Environment environment;

    public IndexController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping({"", "/index"})
    public String index() {
        if (environment.getProperty("agent.onboarding.completed", Boolean.class, false)) {
            return "redirect:/chat";
        }
        return "redirect:/onboarding/";
    }
}
