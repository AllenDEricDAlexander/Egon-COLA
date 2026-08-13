package top.egon.cola.platform.rbac3.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `Rbac3BearerCredentialExtractor` 位于当前包内，是类型，用于承载 `Rbac3 Bearer Credential Extractor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3BearerCredentialExtractor` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Bearer Credential Extractor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Accepts one Authorization Bearer value and never reads credentials from query data.
 */
public final class Rbac3BearerCredentialExtractor
        implements GatewayCredentialExtractor {

    /**
     * 字段 `EXTRACTOR_ID` 表示 `Rbac3BearerCredentialExtractor` 中与 `EXTRACTOR ID` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `EXTRACTOR_ID` stores the `EXTRACTOR ID`-related state, dependency, configuration, or result of `Rbac3BearerCredentialExtractor` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `EXTRACTOR_ID` 时应保持 `Rbac3BearerCredentialExtractor` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `EXTRACTOR_ID`, preserve `Rbac3BearerCredentialExtractor`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String EXTRACTOR_ID = "rbac3-bearer";
    /**
     * 字段 `MAX_TOKEN_LENGTH` 表示 `Rbac3BearerCredentialExtractor` 中与 `MAX TOKEN LENGTH` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_TOKEN_LENGTH` stores the `MAX TOKEN LENGTH`-related state, dependency, configuration, or result of `Rbac3BearerCredentialExtractor` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_TOKEN_LENGTH` 时应保持 `Rbac3BearerCredentialExtractor` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_TOKEN_LENGTH`, preserve `Rbac3BearerCredentialExtractor`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int MAX_TOKEN_LENGTH = 8192;

    /**
     * 字段 `sanitizer` 表示 `Rbac3BearerCredentialExtractor` 中与 `sanitizer` 相关的状态、依赖、配置或结果（声明类型 `Rbac3ReservedHeaderSanitizer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sanitizer` stores the `sanitizer`-related state, dependency, configuration, or result of `Rbac3BearerCredentialExtractor` (declared type `Rbac3ReservedHeaderSanitizer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sanitizer` 时应保持 `Rbac3BearerCredentialExtractor` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sanitizer`, preserve `Rbac3BearerCredentialExtractor`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3ReservedHeaderSanitizer sanitizer;

    /**
     * 构造器 `Rbac3BearerCredentialExtractor` 用于创建并初始化 `Rbac3BearerCredentialExtractor` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3BearerCredentialExtractor` creates and initializes `Rbac3BearerCredentialExtractor`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3BearerCredentialExtractor` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3BearerCredentialExtractor`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sanitizer 输入参数 `sanitizer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3BearerCredentialExtractor(Rbac3ReservedHeaderSanitizer sanitizer) {
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    /**
     * 方法 `extractorId` 按照 `Rbac3BearerCredentialExtractor` 的职责处理输入，完成 `extractor Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `extractorId` processes its inputs according to `Rbac3BearerCredentialExtractor`'s responsibility, performs the `extractor Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `extractorId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `extractorId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String extractorId() {
        return EXTRACTOR_ID;
    }

    /**
     * 方法 `credentialType` 按照 `Rbac3BearerCredentialExtractor` 的职责处理输入，完成 `credential Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `credentialType` processes its inputs according to `Rbac3BearerCredentialExtractor`'s responsibility, performs the `credential Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `credentialType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `credentialType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String credentialType() {
        return "bearer";
    }

    /**
     * 方法 `extract` 按照 `Rbac3BearerCredentialExtractor` 的职责处理输入，完成 `extract` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `extract` processes its inputs according to `Rbac3BearerCredentialExtractor`'s responsibility, performs the `extract` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `extract` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `extract`, then continue the business flow using its result, exception, or side effect.
     *
     * @param exchange 输入参数 `exchange`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policy 输入参数 `policy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Publisher<CredentialExtractionResult> extract(
            GatewayExchange exchange,
            GatewaySecurityPolicy policy
    ) {
        Objects.requireNonNull(exchange, "exchange");
        List<String> values = new ArrayList<>();
        exchange.request().headers().names().stream()
                .filter(name -> "authorization".equalsIgnoreCase(name))
                .forEach(name -> values.addAll(exchange.request().headers().values(name)));
        if (values.isEmpty()) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(), sanitizer.fieldsToRemove(), null));
        }
        if (values.size() != 1) {
            return Mono.just(invalid());
        }
        String value = values.getFirst();
        if (value == null || !value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Mono.just(invalid());
        }
        String token = value.substring(7);
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH
                || token.chars().anyMatch(character -> Character.isWhitespace(character)
                || Character.isISOControl(character) || character == ',')) {
            return Mono.just(invalid());
        }
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", token, Map.of())),
                sanitizer.fieldsToRemove(), null));
    }

    /**
     * 方法 `invalid` 按照 `Rbac3BearerCredentialExtractor` 的职责处理输入，完成 `invalid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalid` processes its inputs according to `Rbac3BearerCredentialExtractor`'s responsibility, performs the `invalid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalid`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private CredentialExtractionResult invalid() {
        return new CredentialExtractionResult(
                List.of(), sanitizer.fieldsToRemove(),
                "GATEWAY_CREDENTIAL_INVALID");
    }
}
