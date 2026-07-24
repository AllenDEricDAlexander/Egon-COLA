package ${package}.start;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

@SpringBootApplication(scanBasePackages = "${package}")
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.user.rpc",
        "${package}.adapter.teaching.rpc"
})
@EnableJpaRepositories(basePackages = {
        "${package}.infrastructure.user.repo.jpa",
        "${package}.infrastructure.teaching.repo.jpa"
}, enableDefaultTransactions = false)
@EntityScan(basePackages = {
        "${package}.infrastructure.user.repo.po",
        "${package}.infrastructure.teaching.repo.po"
})
public class StudentManagementApplication {

    @Bean
    @Profile("!sharding")
    UuidV7Generator uuidV7Generator() {
        return new UuidV7Generator();
    }

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
