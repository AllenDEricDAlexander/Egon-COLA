package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "${package}")
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.user.rpc",
        "${package}.adapter.teaching.rpc"
})
@EnableJpaRepositories(basePackages = {
        "${package}.infrastructure.user.repo.jpa",
        "${package}.infrastructure.teaching.repo.jpa"
})
@EntityScan(basePackages = {
        "${package}.infrastructure.user.repo.po",
        "${package}.infrastructure.teaching.repo.po"
})
public class OrganizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
