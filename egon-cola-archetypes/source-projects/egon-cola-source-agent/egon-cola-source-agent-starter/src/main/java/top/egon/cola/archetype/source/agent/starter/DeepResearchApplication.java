package top.egon.cola.archetype.source.agent.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Boot entry point for the generated Deep Research Agent application. */
@SpringBootApplication(scanBasePackages = "top.egon.cola.archetype.source.agent")
public class DeepResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepResearchApplication.class, args);
    }
}
