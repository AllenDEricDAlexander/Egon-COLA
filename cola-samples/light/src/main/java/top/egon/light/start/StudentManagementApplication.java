package top.egon.light.start;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "top.egon.light")
@EnableDubbo(scanBasePackages = "top.egon.light.adapter.facade")
@EnableJpaRepositories(basePackages = "top.egon.light.infrastructure.repo")
@EntityScan(basePackages = "top.egon.light.infrastructure.repo")
public class StudentManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
