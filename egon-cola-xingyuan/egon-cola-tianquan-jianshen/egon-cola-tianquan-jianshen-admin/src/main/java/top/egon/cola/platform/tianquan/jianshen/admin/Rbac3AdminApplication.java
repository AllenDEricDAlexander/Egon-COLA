package top.egon.cola.platform.tianquan.jianshen.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.controller.cli.Rbac3PlatformAdminBootstrapCli;

import java.util.Map;

/**
 * 类型 `Rbac3AdminApplication` 位于当前包内，是类型，用于承载 `Rbac3 Admin Application` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AdminApplication` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Admin Application`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3AdminApplication` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3AdminApplication` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@lombok.extern.slf4j.Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class Rbac3AdminApplication {

    /**
     * 方法 `main` 按照 `Rbac3AdminApplication` 的职责处理输入，完成 `main` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `main` processes its inputs according to `Rbac3AdminApplication`'s responsibility, performs the `main` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `main` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `main`, then continue the business flow using its result, exception, or side effect.
     *
     * @param args 输入参数 `args`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public static void main(String[] args) {
        if (isCatalogCommand(args)) {
            SpringApplication preflight = new SpringApplication(CatalogInputConfiguration.class);
            preflight.setWebApplicationType(WebApplicationType.NONE);
            preflight.setAdditionalProfiles("rbac3-catalog-input");
            preflight.setLogStartupInfo(false);
            preflight.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
            int inputResult;
            try (var input = preflight.run()) {
                inputResult = top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.validateLocalInvocation(
                        args, input.getBean("jacksonObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class),
                        input.getBean("defaultValidator", jakarta.validation.Validator.class));
            }
            if (inputResult != 0) { System.exit(inputResult); return; }
            SpringApplication application = new SpringApplication(
                    top.egon.cola.platform.tianquan.jianshen.admin.config.runtime.Rbac3ApplicationConfiguration.CatalogCommandConfiguration.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setAdditionalProfiles("rbac3-catalog-command");
            application.setLogStartupInfo(false);
            application.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
            application.addInitializers(context -> context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("rbac3CatalogCommandRuntime", catalogCommandRuntimeProperties())));
            application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("rbac3FieldRuleCommandRuntime",
                    Map.of("rbac3.catalog.field-rules-enabled", args[0].equals(top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.FIELD_COMMAND)))));
            int result;
            try (var context = application.run(args)) {
                result = context.getBean("rbac3ApplicationCatalogRegistrationCli",
                        top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.class).run(args);
            }
            if (result != 0) { System.exit(result); }
            return;
        }
        if (isBootstrapCommand(args)) {
            SpringApplication application = new SpringApplication(Rbac3AdminApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.addInitializers(context -> context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            "rbac3BootstrapRuntime",
                            bootstrapRuntimeProperties())));
            try (var context = application.run(args)) {
                context.getBean(Rbac3PlatformAdminBootstrapCli.class)
                        .run(args);
            }
            return;
        }
        SpringApplication.run(Rbac3AdminApplication.class, args);
    }

    /**
     * 方法 `isBootstrapCommand` 按照 `Rbac3AdminApplication` 的职责处理输入，完成 `is Bootstrap Command` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isBootstrapCommand` processes its inputs according to `Rbac3AdminApplication`'s responsibility, performs the `is Bootstrap Command` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isBootstrapCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isBootstrapCommand`, then continue the business flow using its result, exception, or side effect.
     *
     * @param args 输入参数 `args`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    static boolean isBootstrapCommand(String[] args) {
        return args.length > 0 && "bootstrap-platform-admin".equals(args[0]);
    }

    /**
     * 方法 `bootstrapRuntimeProperties` 按照 `Rbac3AdminApplication` 的职责处理输入，完成 `bootstrap Runtime Properties` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrapRuntimeProperties` processes its inputs according to `Rbac3AdminApplication`'s responsibility, performs the `bootstrap Runtime Properties` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrapRuntimeProperties` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrapRuntimeProperties`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    static Map<String, Object> bootstrapRuntimeProperties() {
        return Map.of(
                "egon.cola.component.tianshu.enabled", false,
                "egon.cola.component.yuheng.reporting.enabled", false,
                "egon.cola.component.tianshu.registry.http.enabled", false,
                "egon.cola.component.transactional-outbox.polling.enabled", false,
                "management.endpoint.health.validate-group-membership", false);
    }
    public static boolean isCatalogCommand(String[] args) {
        return args != null && args.length > 0 && (top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.COMMAND.equals(args[0])
                || top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.FIELD_COMMAND.equals(args[0]));
    }

    public static Map<String, Object> catalogCommandRuntimeProperties() {
        return Map.ofEntries(
                Map.entry("egon.cola.component.rpc.provider.enabled", false),
                Map.entry("egon.cola.component.rpc.provider.grpc.enabled", false),
                Map.entry("egon.cola.component.tianshu.enabled", false),
                Map.entry("egon.cola.component.tianshu.registry.enabled", false),
                Map.entry("egon.cola.component.tianshu.registry.http.enabled", false),
                Map.entry("egon.cola.component.yuheng.reporting.enabled", false),
                Map.entry("egon.cola.component.transactional-outbox.polling.enabled", false),
                Map.entry("egon.cola.component.transactional-outbox.storage.mp.auto-initialize-ddl", false),
                Map.entry("egon.cola.component.mybatis-plus.ddl.enabled", false),
                Map.entry("rbac3.bootstrap.auto-activate-local-admin-roles", false),
                Map.entry("spring.main.web-application-type", "none"),
                Map.entry("spring.jpa.hibernate.ddl-auto", "none"));
    }

    /** Local preflight deliberately has no datasource, RPC, cache or application component imports. */
    @lombok.extern.slf4j.Slf4j
    @org.springframework.context.annotation.Configuration(value = "rbac3CatalogInputConfiguration", proxyBeanMethods = false)
    @org.springframework.context.annotation.Profile("rbac3-catalog-input")
    @org.springframework.boot.autoconfigure.ImportAutoConfiguration({
            org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
            org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class})
    public static class CatalogInputConfiguration { }

}
