package top.egon.fable.web.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "top.egon.fable.web")
@EnableDubbo(scanBasePackages = "top.egon.fable.web.adapter.facade")
@EnableJpaRepositories(basePackages = "top.egon.fable.web.infrastructure.repo")
@EntityScan(basePackages = "top.egon.fable.web.infrastructure.repo")
public class OrganizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
