#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "${package}")
@EnableDubbo(scanBasePackages = "${package}.adapter.facade")
@EntityScan(basePackages = {
        "${package}.infrastructure.course.repo",
        "${package}.infrastructure.exam.repo"
})
@EnableJpaRepositories(basePackages = {
        "${package}.infrastructure.course.repo",
        "${package}.infrastructure.exam.repo"
})
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
