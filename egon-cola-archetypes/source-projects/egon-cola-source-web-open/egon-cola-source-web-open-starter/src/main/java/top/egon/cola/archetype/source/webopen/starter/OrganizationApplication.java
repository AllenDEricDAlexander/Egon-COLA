package top.egon.cola.archetype.source.webopen.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.cola.archetype.source.webopen")
@EnableDubbo(scanBasePackages = {
        "top.egon.cola.archetype.source.webopen.adapter.user.rpc",
        "top.egon.cola.archetype.source.webopen.adapter.teaching.rpc"
})
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.archetype.source.webopen.infrastructure.user.dao",
        "top.egon.cola.archetype.source.webopen.infrastructure.teaching.dao"
})
public class OrganizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
