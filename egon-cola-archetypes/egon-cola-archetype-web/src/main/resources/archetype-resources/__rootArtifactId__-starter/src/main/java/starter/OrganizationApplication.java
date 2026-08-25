package ${package}.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "${package}",
        exclude = FlywayAutoConfiguration.class)
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.user.rpc",
        "${package}.adapter.teaching.rpc"
})
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "${package}.infrastructure.user.repo.dao",
        "${package}.infrastructure.teaching.repo.dao"
})
public class OrganizationApplication {

    @Bean
    LongIdGenerator longIdGenerator() {
        return () -> 2001L;
    }

    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
