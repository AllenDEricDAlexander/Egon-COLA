package top.egon.cola.archetype.source.light.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.cola.archetype.source.light")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.archetype.source.light.infrastructure.user.dao",
        "top.egon.cola.archetype.source.light.infrastructure.teaching.dao"
})
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
