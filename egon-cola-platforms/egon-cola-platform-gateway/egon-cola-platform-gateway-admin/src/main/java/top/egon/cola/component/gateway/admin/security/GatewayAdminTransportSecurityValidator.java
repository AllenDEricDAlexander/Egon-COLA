package top.egon.cola.component.gateway.admin.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayAdminTransportSecurityValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责网关管理端传输安全校验器相关的职责与边界。
 * English summary: {@code GatewayAdminTransportSecurityValidator} is a gateway admin transport security validator validator in the current Gateway module; it owns the gateway admin transport security validator-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public final class GatewayAdminTransportSecurityValidator
        implements ApplicationRunner {

    /**
     * 中文说明：表示 PREFIX 这一固定值；它属于 {@code GatewayAdminTransportSecurityValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value prefix; it is a state, type, or protocol value of {@code GatewayAdminTransportSecurityValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminTransportSecurityValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminTransportSecurityValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String PREFIX =
            "gateway.admin.transport-security";

    /**
     * 中文说明：保存 environment 对应的状态、依赖或配置值；字段类型为 {@code Environment}，由 {@code GatewayAdminTransportSecurityValidator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by environment; its type is {@code Environment}, and {@code GatewayAdminTransportSecurityValidator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminTransportSecurityValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminTransportSecurityValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Environment environment;

    /**
     * 中文说明：创建 {@code GatewayAdminTransportSecurityValidator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAdminTransportSecurityValidator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param environment 参数 environment；parameter environment。
     */
    public GatewayAdminTransportSecurityValidator(Environment environment) {
        this.environment = Objects.requireNonNull(
                environment,
                "environment"
        );
    }

    /**
     * 中文说明：执行 run 操作；该方法是 {@code GatewayAdminTransportSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the run operation; this method is the invocation entry point on {@code GatewayAdminTransportSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminTransportSecurityValidator.run(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param args 参数 args；parameter args。
     */
    @Override
    public void run(ApplicationArguments args) {
        validate(environment, PREFIX);
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayAdminTransportSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayAdminTransportSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminTransportSecurityValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param environment 参数 environment；parameter environment。
     * @param prefix 参数 prefix；parameter prefix。
     */
    static void validate(Environment environment, String prefix) {
        String mode = required(environment, prefix + ".mode")
                .toUpperCase(Locale.ROOT);
        if ("DEVELOPMENT_PLAINTEXT".equals(mode)) {
            return;
        }
        if (!"MTLS".equals(mode)) {
            throw new IllegalStateException(
                    prefix + ".mode must be DEVELOPMENT_PLAINTEXT or MTLS"
            );
        }
        if (!environment.getProperty(
                "server.ssl.enabled",
                Boolean.class,
                false
        )) {
            throw new IllegalStateException(
                    "MTLS requires server.ssl.enabled=true"
            );
        }
        if (!"need".equalsIgnoreCase(environment.getProperty(
                "server.ssl.client-auth",
                ""
        ))) {
            throw new IllegalStateException(
                    "MTLS requires server.ssl.client-auth=need"
            );
        }
        String bundle = required(environment, "server.ssl.bundle");
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".keystore.certificate"
        );
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".keystore.private-key"
        );
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".truststore.certificate"
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayAdminTransportSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayAdminTransportSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminTransportSecurityValidator.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param environment 参数 environment；parameter environment。
     * @param property 参数 property；parameter property。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(
            Environment environment,
            String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " is required");
        }
        return value.trim();
    }
}
