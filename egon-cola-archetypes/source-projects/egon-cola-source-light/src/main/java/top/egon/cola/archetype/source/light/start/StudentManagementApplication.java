package top.egon.cola.archetype.source.light.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.cola.archetype.source.light")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.archetype.source.light.infrastructure.user.repo.dao",
        "top.egon.cola.archetype.source.light.infrastructure.teaching.repo.dao"
})
public class StudentManagementApplication {

    @Bean("snowflakeIdGenerator")
    @Profile("test")
    LongIdGenerator snowflakeIdGenerator() {
        java.util.concurrent.atomic.AtomicLong values = new java.util.concurrent.atomic.AtomicLong(100000);
        return values::incrementAndGet;
    }

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
