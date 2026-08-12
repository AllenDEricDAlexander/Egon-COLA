package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.time.Instant;
import java.util.List;

/**
 * Exposes identity and authorization facts from the verified token.
 * 补充说明 / Supplementary summary: {@code GatewayAdminSessionController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关管理端会话控制器相关的职责与边界。
 * English supplement: {@code GatewayAdminSessionController} is a gateway admin session controller controller in the current Gateway module; it owns the gateway admin session controller-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/session")
public class GatewayAdminSessionController {

    /**
     * 中文说明：执行 会话 操作；该方法是 {@code GatewayAdminSessionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the session operation; this method is the invocation entry point on {@code GatewayAdminSessionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminSessionController.session(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 会话 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public SessionView session(
            AdminActor actor,
            Authentication authentication) {
        String displayName = actor.actorId();
        Instant expiresAt = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            displayName = displayName(jwt, actor.actorId());
            expiresAt = jwt.getToken().getExpiresAt();
        }
        return new SessionView(
                actor.actorId(),
                displayName,
                actor.actorType().name(),
                actor.scopes().stream().sorted().toList(),
                actor.roles().stream().sorted().toList(),
                expiresAt
        );
    }

    /**
     * 中文说明：执行 displayName 操作；该方法是 {@code GatewayAdminSessionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the display name operation; this method is the invocation entry point on {@code GatewayAdminSessionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminSessionController.displayName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param jwt 参数 jwt；parameter jwt。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 displayName 的处理结果；returns the result of the operation.
     */
    private String displayName(
            JwtAuthenticationToken jwt,
            String fallback) {
        String preferred = jwt.getToken().getClaimAsString(
                "preferred_username"
        );
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        String name = jwt.getToken().getClaimAsString("name");
        return name == null || name.isBlank() ? fallback : name;
    }

    /**
     * 中文说明：{@code SessionView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责会话View相关的职责与边界。
     * English summary: {@code SessionView} is an immutable data carrier in the current Gateway module; it owns the session view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param actorId 参数 actorId；parameter actor id。
     * @param displayName 参数 displayName；parameter display name。
     * @param actorType 参数 actorType；parameter actor type。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param roles 参数 roles；parameter roles。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     */
    public record SessionView(
            /**
             * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String actorId,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 actorType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by actor type; its type is {@code String}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String actorType,
            /**
             * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code List<String>}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<String> capabilities,
            /**
             * 中文说明：保存 roles 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by roles; its type is {@code List<String>}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<String> roles,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayAdminSessionController.SessionView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code GatewayAdminSessionController.SessionView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayAdminSessionController.SessionView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminSessionController.SessionView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt
    ) {
    }
}
