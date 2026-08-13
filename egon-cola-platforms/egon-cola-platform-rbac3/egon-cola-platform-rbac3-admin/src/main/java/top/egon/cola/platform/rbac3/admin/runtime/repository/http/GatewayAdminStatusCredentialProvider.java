package top.egon.cola.platform.rbac3.admin.runtime.repository.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.BearerCredentialVO;

/**
 * 类型 `GatewayAdminStatusCredentialProvider` 位于当前包内，是接口，用于承载 `Gateway Admin Status Credential Provider` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `GatewayAdminStatusCredentialProvider` is an interface in its package and carries the responsibility, state, or contract for `Gateway Admin Status Credential Provider`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Supplies a short-lived read-only OAuth credential without storing it in properties.
 */
@FunctionalInterface
public interface GatewayAdminStatusCredentialProvider {

    /**
     * 方法 `current` 按照 `GatewayAdminStatusCredentialProvider` 的职责处理输入，完成 `current` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `current` processes its inputs according to `GatewayAdminStatusCredentialProvider`'s responsibility, performs the `current` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `current` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `current`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    Optional<BearerCredentialVO> current();

    /**
     * 方法 `rotatingFile` 按照 `GatewayAdminStatusCredentialProvider` 的职责处理输入，完成 `rotating File` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotatingFile` processes its inputs according to `GatewayAdminStatusCredentialProvider`'s responsibility, performs the `rotating File` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotatingFile` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotatingFile`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tokenFile 输入参数 `tokenFile`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    static GatewayAdminStatusCredentialProvider rotatingFile(
            Path tokenFile,
            ObjectMapper objectMapper,
            Clock clock) {
        Objects.requireNonNull(tokenFile, "tokenFile");
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(clock, "clock");
        return () -> {
            if (!Files.isRegularFile(tokenFile)) {
                return Optional.empty();
            }
            try {
                JsonNode json = objectMapper.readTree(Files.readString(tokenFile));
                String token = json.path("accessToken").asText();
                String expiresAt = json.path("expiresAt").asText();
                if (token.isBlank() || expiresAt.isBlank()) {
                    return Optional.empty();
                }
                BearerCredentialVO credential = new BearerCredentialVO(
                        token, Instant.parse(expiresAt));
                return credential.expiresAt().isAfter(clock.instant())
                        ? Optional.of(credential)
                        : Optional.empty();
            } catch (Exception invalid) {
                return Optional.empty();
            }
        };
    }

    }
