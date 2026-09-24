package top.egon.cola.component.yuheng.admin.bootstrap;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

/**
 * 中文说明：{@code GatewayAdminApplication} 是类型，位于当前 Gateway 模块的相关包中，负责网关管理端Application相关的职责与边界。
 * 持久化唯一入口是 Egon MyBatis-Plus 受守卫边界：实体由 {@link MapperScan} 逐包注册，JPA 实体扫描与
 * {@code @EnableJpaRepositories} 已随 MP 切换一并移除，运行期不再存在第二套 ORM 或 Flyway 迁移链路。
 * English summary: {@code GatewayAdminApplication} is a type in the current Gateway module; it owns the gateway admin
 * application-related responsibility and boundary. MyBatis-Plus is the only persistence entry point: mappers are
 * registered package by package via {@link MapperScan}, and the JPA entity scan plus {@code @EnableJpaRepositories}
 * were removed with the MP cutover, so no second ORM or Flyway runtime remains at execution time.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@SpringBootApplication(scanBasePackages = GatewayAdminApplication.ADMIN_PACKAGE)
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.cola.component.yuheng.admin.application.dao",
        "top.egon.cola.component.yuheng.admin.catalog.dao",
        "top.egon.cola.component.yuheng.admin.credential.dao",
        "top.egon.cola.component.yuheng.admin.group.dao",
        "top.egon.cola.component.yuheng.admin.knowledge.dao",
        "top.egon.cola.component.yuheng.admin.llm.dao",
        "top.egon.cola.component.yuheng.admin.mcp.dao",
        "top.egon.cola.component.yuheng.admin.observability.dao",
        "top.egon.cola.component.yuheng.admin.openapi.dao",
        "top.egon.cola.component.yuheng.admin.release.dao",
        "top.egon.cola.component.yuheng.admin.reporting.dao",
        "top.egon.cola.component.yuheng.admin.routing.dao",
        "top.egon.cola.component.yuheng.admin.shared.dao",
        "top.egon.cola.component.yuheng.admin.wiki.dao"
})
public class GatewayAdminApplication {

    public static final String ADMIN_PACKAGE = "top.egon.cola.component.yuheng.admin";

    /**
     * 中文说明：执行 main 操作；该方法是 {@code GatewayAdminApplication} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the main operation; this method is the invocation entry point on {@code GatewayAdminApplication} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminApplication.main(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param args 参数 args；parameter args。
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayAdminApplication.class, args);
    }
}
