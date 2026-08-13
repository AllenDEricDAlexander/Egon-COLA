package top.egon.cola.platform.rbac3.admin.integration.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * 类型 `Rbac3FlywayConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Flyway Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3FlywayConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Flyway Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Owns the independent RBAC3 and Transactional Outbox migration histories.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3FlywayConfiguration {

    /**
     * 字段 `RBAC3_FLYWAY` 表示 `Rbac3FlywayConfiguration` 中与 `RBAC3 FLYWAY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `RBAC3_FLYWAY` stores the `RBAC3 FLYWAY`-related state, dependency, configuration, or result of `Rbac3FlywayConfiguration` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `RBAC3_FLYWAY` 时应保持 `Rbac3FlywayConfiguration` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RBAC3_FLYWAY`, preserve `Rbac3FlywayConfiguration`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String RBAC3_FLYWAY = "rbac3Flyway";
    /**
     * 字段 `OUTBOX_FLYWAY` 表示 `Rbac3FlywayConfiguration` 中与 `OUTBOX FLYWAY` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `OUTBOX_FLYWAY` stores the `OUTBOX FLYWAY`-related state, dependency, configuration, or result of `Rbac3FlywayConfiguration` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `OUTBOX_FLYWAY` 时应保持 `Rbac3FlywayConfiguration` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `OUTBOX_FLYWAY`, preserve `Rbac3FlywayConfiguration`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String OUTBOX_FLYWAY = "outboxFlyway";
    /**
     * 字段 `RBAC3_INITIALIZER` 表示 `Rbac3FlywayConfiguration` 中与 `RBAC3 INITIALIZER` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `RBAC3_INITIALIZER` stores the `RBAC3 INITIALIZER`-related state, dependency, configuration, or result of `Rbac3FlywayConfiguration` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `RBAC3_INITIALIZER` 时应保持 `Rbac3FlywayConfiguration` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RBAC3_INITIALIZER`, preserve `Rbac3FlywayConfiguration`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String RBAC3_INITIALIZER = "rbac3FlywayInitializer";
    /**
     * 字段 `OUTBOX_INITIALIZER` 表示 `Rbac3FlywayConfiguration` 中与 `OUTBOX INITIALIZER` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `OUTBOX_INITIALIZER` stores the `OUTBOX INITIALIZER`-related state, dependency, configuration, or result of `Rbac3FlywayConfiguration` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `OUTBOX_INITIALIZER` 时应保持 `Rbac3FlywayConfiguration` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `OUTBOX_INITIALIZER`, preserve `Rbac3FlywayConfiguration`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String OUTBOX_INITIALIZER = "outboxFlywayInitializer";

    /**
     * 方法 `rbac3Flyway` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `rbac3 Flyway` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3Flyway` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `rbac3 Flyway` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3Flyway` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3Flyway`, then continue the business flow using its result, exception, or side effect.
     *
     * @param dataSource 输入参数 `dataSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(RBAC3_FLYWAY)
    Flyway rbac3Flyway(DataSource dataSource) {
        return buildRbac3Flyway(dataSource);
    }

    /**
     * 方法 `outboxFlyway` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `outbox Flyway` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `outboxFlyway` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `outbox Flyway` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `outboxFlyway` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `outboxFlyway`, then continue the business flow using its result, exception, or side effect.
     *
     * @param dataSource 输入参数 `dataSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(OUTBOX_FLYWAY)
    Flyway outboxFlyway(DataSource dataSource) {
        return buildOutboxFlyway(dataSource);
    }

    /**
     * 方法 `rbac3FlywayInitializer` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `rbac3 Flyway Initializer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3FlywayInitializer` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `rbac3 Flyway Initializer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3FlywayInitializer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3FlywayInitializer`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rbac3Flyway 输入参数 `rbac3Flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(RBAC3_INITIALIZER)
    FlywayMigrationInitializer rbac3FlywayInitializer(
            @Qualifier(RBAC3_FLYWAY) Flyway rbac3Flyway) {
        return new FlywayMigrationInitializer(rbac3Flyway);
    }

    /**
     * 方法 `outboxFlywayInitializer` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `outbox Flyway Initializer` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `outboxFlywayInitializer` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `outbox Flyway Initializer` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `outboxFlywayInitializer` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `outboxFlywayInitializer`, then continue the business flow using its result, exception, or side effect.
     *
     * @param outboxFlyway 输入参数 `outboxFlyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(OUTBOX_INITIALIZER)
    FlywayMigrationInitializer outboxFlywayInitializer(
            @Qualifier(OUTBOX_FLYWAY) Flyway outboxFlyway) {
        return new FlywayMigrationInitializer(outboxFlyway);
    }

    /**
     * 方法 `rbac3MigrationDependencies` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `rbac3 Migration Dependencies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3MigrationDependencies` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `rbac3 Migration Dependencies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     * Forces JPA validation and Outbox schema validation behind both migrations.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    static BeanFactoryPostProcessor rbac3MigrationDependencies() {
        return beanFactory -> {
            dependOn(beanFactory, "entityManagerFactory");
            dependOn(beanFactory, "outboxSchemaValidator");
        };
    }

    /**
     * 方法 `buildRbac3Flyway` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `build Rbac3 Flyway` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `buildRbac3Flyway` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `build Rbac3 Flyway` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `buildRbac3Flyway` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `buildRbac3Flyway`, then continue the business flow using its result, exception, or side effect.
     *
     * @param dataSource 输入参数 `dataSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static Flyway buildRbac3Flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table("flyway_schema_history_rbac3")
                .validateMigrationNaming(true)
                .load();
    }

    /**
     * 方法 `buildOutboxFlyway` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `build Outbox Flyway` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `buildOutboxFlyway` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `build Outbox Flyway` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `buildOutboxFlyway` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `buildOutboxFlyway`, then continue the business flow using its result, exception, or side effect.
     *
     * @param dataSource 输入参数 `dataSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static Flyway buildOutboxFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/transactional-outbox/postgresql")
                .table("flyway_schema_history_outbox")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .validateMigrationNaming(true)
                .load();
    }

    /**
     * 方法 `dependOn` 按照 `Rbac3FlywayConfiguration` 的职责处理输入，完成 `depend On` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dependOn` processes its inputs according to `Rbac3FlywayConfiguration`'s responsibility, performs the `depend On` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dependOn` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dependOn`, then continue the business flow using its result, exception, or side effect.
     *
     * @param factory 输入参数 `factory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param beanName 输入参数 `beanName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void dependOn(
            org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory,
            String beanName) {
        if (!factory.containsBeanDefinition(beanName)) {
            return;
        }
        BeanDefinition definition = factory.getBeanDefinition(beanName);
        var dependencies = new LinkedHashSet<String>();
        if (definition.getDependsOn() != null) {
            dependencies.addAll(Arrays.asList(definition.getDependsOn()));
        }
        dependencies.add(RBAC3_INITIALIZER);
        dependencies.add(OUTBOX_INITIALIZER);
        definition.setDependsOn(dependencies.toArray(String[]::new));
    }
}
