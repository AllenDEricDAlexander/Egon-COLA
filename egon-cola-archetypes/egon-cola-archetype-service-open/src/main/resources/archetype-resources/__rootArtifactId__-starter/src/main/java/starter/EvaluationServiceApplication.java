#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@SpringBootApplication(
        scanBasePackages = "${package}",
        exclude = FlywayAutoConfiguration.class)
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
    @Profile("test")
    LongIdGenerator testLongIdGenerator() {
        return () -> 2001L;
    }

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
