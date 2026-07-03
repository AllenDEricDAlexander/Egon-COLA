package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "${package}")
@EnableDubbo(scanBasePackages = "${package}.adapter.facade")
@EnableJpaRepositories(basePackages = "${package}.infrastructure.repo")
@EntityScan(basePackages = "${package}.infrastructure.repo")
public class OrganizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
