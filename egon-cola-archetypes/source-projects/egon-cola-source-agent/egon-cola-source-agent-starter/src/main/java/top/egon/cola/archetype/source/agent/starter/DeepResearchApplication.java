package top.egon.cola.archetype.source.agent.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Boot entry point for the generated Deep Research Agent application. */
@SpringBootApplication(scanBasePackages = "top.egon.cola.archetype.source.agent")
// The data access interfaces live in Infrastructure, outside the base package the automatic
// scanning of the starter covers, so each repository package is registered here.
@MapperScan(basePackages = "top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao")
public class DeepResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepResearchApplication.class, args);
    }
}
