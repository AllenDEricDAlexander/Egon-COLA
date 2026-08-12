package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayCredentialStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关凭证存储相关的职责与边界。
 * English summary: {@code JdbcGatewayCredentialStore} is a jdbc gateway credential store store in the current Gateway module; it owns the jdbc gateway credential store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayCredentialStore implements GatewayCredentialStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayCredentialStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayCredentialStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayCredentialStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCredentialStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcGatewayCredentialStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayCredentialStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcGatewayCredentialStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param credential 参数 凭证；parameter credential。
     */
    @Override
    public void insert(CredentialRecord credential) {
        jdbc.update("""
                INSERT INTO gateway_application_credential(
                    id, application_id, access_key, secret_ciphertext,
                    secret_reference, key_version, status, valid_from,
                    valid_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)
                """,
                credential.id(),
                credential.applicationId(),
                credential.accessKey(),
                credential.secretCiphertext(),
                credential.keyVersion(),
                credential.status(),
                timestamp(credential.validFrom()),
                timestamp(credential.validUntil()),
                timestamp(credential.createdAt()),
                timestamp(credential.updatedAt())
        );
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<CredentialRecord> find(
            String applicationId,
            String keyId) {
        return query("""
                SELECT id, application_id, access_key, secret_ciphertext,
                       key_version, status, valid_from, valid_until,
                       created_at, updated_at
                  FROM gateway_application_credential
                 WHERE application_id = ?
                   AND (id = ? OR access_key = ?)
                """, applicationId, keyId, keyId);
    }

    /**
     * 中文说明：执行 findByAccess键 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by access key operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.findByAccessKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessKey 参数 access键；parameter access key。
     * @return 返回 findByAccess键 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<CredentialRecord> findByAccessKey(String accessKey) {
        return query("""
                SELECT id, application_id, access_key, secret_ciphertext,
                       key_version, status, valid_from, valid_until,
                       created_at, updated_at
                  FROM gateway_application_credential
                 WHERE access_key = ?
                """, accessKey);
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Override
    public List<CredentialRecord> list(String applicationId) {
        return queryAll("""
                SELECT id, application_id, access_key, secret_ciphertext,
                       key_version, status, valid_from, valid_until,
                       created_at, updated_at
                  FROM gateway_application_credential
                 WHERE application_id = ?
                 ORDER BY created_at DESC, id DESC
                """, applicationId);
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sql 参数 sql；parameter sql。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private Optional<CredentialRecord> query(
            String sql,
            Object... arguments) {
        return queryAll(sql, arguments).stream().findFirst();
    }

    /**
     * 中文说明：执行 queryAll 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query all operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.queryAll(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sql 参数 sql；parameter sql。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 queryAll 的处理结果；returns the result of the operation.
     */
    private List<CredentialRecord> queryAll(
            String sql,
            Object... arguments) {
        return jdbc.query(sql, (result, row) -> new CredentialRecord(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("access_key"),
                result.getString("secret_ciphertext"),
                result.getString("key_version"),
                result.getString("status"),
                result.getTimestamp("valid_from").toInstant(),
                result.getTimestamp("valid_until") == null
                        ? null
                        : result.getTimestamp("valid_until").toInstant(),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        ), arguments);
    }

    /**
     * 中文说明：执行 overlap 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the overlap operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.overlap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param validUntil 参数 validUntil；parameter valid until。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void overlap(String id, Instant validUntil, Instant now) {
        jdbc.update("""
                UPDATE gateway_application_credential
                   SET status = 'ROTATING', valid_until = ?, updated_at = ?
                 WHERE id = ? AND status IN ('ACTIVE', 'ROTATING')
                """, timestamp(validUntil), timestamp(now), id);
    }

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code JdbcGatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code JdbcGatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCredentialStore.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void revoke(String id, Instant now) {
        jdbc.update("""
                UPDATE gateway_application_credential
                   SET status = 'REVOKED', valid_until = ?, updated_at = ?
                 WHERE id = ?
                """, timestamp(now), timestamp(now), id);
    }
}
