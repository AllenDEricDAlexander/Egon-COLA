package top.egon.cola.archetype.source.serviceopen.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.cola.archetype.source.serviceopen")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@EnableDubbo(scanBasePackages = {
        "top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl",
        "top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl"
})
@MapperScan(basePackages = {
        "top.egon.cola.archetype.source.serviceopen.infrastructure.course.dao",
        "top.egon.cola.archetype.source.serviceopen.infrastructure.exam.dao"
})
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
