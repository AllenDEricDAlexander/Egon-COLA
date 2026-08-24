package ${package}.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

@SpringBootApplication(
        scanBasePackages = "${package}")
@EnableDubbo(scanBasePackages = {
        "${package}.adapter.user.rpc",
        "${package}.adapter.teaching.rpc"
})
@MapperScan(basePackages = {
        "${package}.infrastructure.user.repo.mapper",
        "${package}.infrastructure.teaching.repo.mapper"
})
public class OrganizationApplication {

    @Bean
    @Profile("test")
    LongIdGenerator testLongIdGenerator() {
        return new SnowflakeIdGenerator(0L);
    }

    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
