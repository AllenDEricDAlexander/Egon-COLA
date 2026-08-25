#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "${package}")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.course.facade.impl",
        "${package}.adapter.exam.facade.impl"
})
@MapperScan(basePackages = {
        "${package}.infrastructure.course.repo.dao",
        "${package}.infrastructure.exam.repo.dao"
})
public class EvaluationServiceApplication {

    @Bean
    @Profile("test")
    LongIdGenerator snowflakeIdGenerator() {
        return () -> 2001L;
    }

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
