#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

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
        "${package}.adapter.course.facade.impl",
        "${package}.adapter.exam.facade.impl"
})
@EntityScan(basePackages = {
        "${package}.infrastructure.course.repo",
        "${package}.infrastructure.exam.repo"
})
@EnableJpaRepositories(basePackages = {
        "${package}.infrastructure.course.repo",
        "${package}.infrastructure.exam.repo"
}, enableDefaultTransactions = false)
public class EvaluationServiceApplication {

    @Bean
    @Profile("!sharding")
    UuidV7Generator uuidV7Generator() {
        return new UuidV7Generator();
    }

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
