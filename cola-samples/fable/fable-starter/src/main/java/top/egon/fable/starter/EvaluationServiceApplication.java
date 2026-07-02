package top.egon.fable.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "top.egon.fable")
@EntityScan(basePackages = "top.egon.fable.infrastructure.repo")
@EnableJpaRepositories(basePackages = "top.egon.fable.infrastructure.repo")
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
