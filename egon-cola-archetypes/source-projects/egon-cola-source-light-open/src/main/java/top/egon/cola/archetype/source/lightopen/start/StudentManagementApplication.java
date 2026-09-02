package top.egon.cola.archetype.source.lightopen.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.cola.archetype.source.lightopen")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao",
        "top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao"
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
