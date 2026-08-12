package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;

import java.time.Instant;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayHmacNonceStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关HmacNonce存储相关的职责与边界。
 * English summary: {@code JdbcGatewayHmacNonceStore} is a jdbc gateway hmac nonce store store in the current Gateway module; it owns the jdbc gateway hmac nonce store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayHmacNonceStore implements GatewayHmacNonceStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayHmacNonceStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayHmacNonceStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayHmacNonceStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayHmacNonceStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcGatewayHmacNonceStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayHmacNonceStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcGatewayHmacNonceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 中文说明：执行 claim 操作；该方法是 {@code JdbcGatewayHmacNonceStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the claim operation; this method is the invocation entry point on {@code JdbcGatewayHmacNonceStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayHmacNonceStore.claim(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessKey 参数 access键；parameter access key。
     * @param nonce 参数 nonce；parameter nonce。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @param now 参数 now；parameter now。
     * @return 返回 claim 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean claim(
            String accessKey,
            String nonce,
            Instant expiresAt,
            Instant now) {
        try {
            jdbc.update("""
                    INSERT INTO gateway_hmac_nonce(
                        access_key, nonce, expires_at, created_at
                    ) VALUES (?, ?, ?, ?)
                    """, accessKey, nonce, timestamp(expiresAt),
                    timestamp(now));
            return true;
        } catch (DataIntegrityViolationException replay) {
            return false;
        }
    }

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code JdbcGatewayHmacNonceStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code JdbcGatewayHmacNonceStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayHmacNonceStore.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    @Override
    public int deleteExpired(Instant now) {
        return jdbc.update(
                "DELETE FROM gateway_hmac_nonce WHERE expires_at < ?",
                timestamp(now)
        );
    }
}
