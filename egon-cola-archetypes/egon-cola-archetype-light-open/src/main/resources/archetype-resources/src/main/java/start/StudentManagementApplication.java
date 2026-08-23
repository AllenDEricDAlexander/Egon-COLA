package ${package}.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@SpringBootApplication(
        scanBasePackages = "${package}")
@MapperScan(basePackages = {
        "${package}.infrastructure.user.repo.mapper",
        "${package}.infrastructure.teaching.repo.mapper"
})
public class StudentManagementApplication {

    @Bean
    @Profile("test")
    LongIdGenerator testLongIdGenerator() {
        return () -> 2001L;
    }

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
