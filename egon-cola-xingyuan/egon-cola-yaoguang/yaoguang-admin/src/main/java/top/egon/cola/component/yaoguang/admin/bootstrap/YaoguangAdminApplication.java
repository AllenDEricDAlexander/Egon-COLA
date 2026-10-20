package top.egon.cola.component.yaoguang.admin.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

/**
 * 中文说明：摇光知识库与 Wiki 的独立管理宿主，统一使用受守卫 MyBatis-Plus 和本地事务。
 * English summary: Independent knowledge and Wiki host using guarded MyBatis-Plus and local transactions.
 */
@SpringBootApplication(scanBasePackages = "top.egon.cola.component.yaoguang.admin")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.component.yaoguang.admin.knowledge.dao",
        "top.egon.cola.component.yaoguang.admin.wiki.dao",
        "top.egon.cola.component.yaoguang.admin.shared.dao",
        "top.egon.cola.component.yaoguang.admin.observability.dao"
})
public class YaoguangAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaoguangAdminApplication.class, args);
    }
}
