package ${package}.start;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "${package}",
        exclude = FlywayAutoConfiguration.class)
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.user.rpc",
        "${package}.adapter.teaching.rpc"
})
@MapperScan(basePackages = {
        "${package}.infrastructure.user.repo.dao",
        "${package}.infrastructure.teaching.repo.dao"
})
public class StudentManagementApplication {

    @Bean
    @Profile("test")
    LongIdGenerator snowflakeIdGenerator() {
        return () -> 2001L;
    }

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
