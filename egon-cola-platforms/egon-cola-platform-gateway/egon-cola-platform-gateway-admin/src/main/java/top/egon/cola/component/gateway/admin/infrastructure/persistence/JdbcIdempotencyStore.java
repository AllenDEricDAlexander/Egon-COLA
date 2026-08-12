package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;

import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

/**
 * 中文说明：{@code JdbcIdempotencyStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcIdempotency存储相关的职责与边界。
 * English summary: {@code JdbcIdempotencyStore} is a jdbc idempotency store store in the current Gateway module; it owns the jdbc idempotency store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcIdempotencyStore implements IdempotencyStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcIdempotencyStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcIdempotencyStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcIdempotencyStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcIdempotencyStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcIdempotencyStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code JdbcIdempotencyStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcIdempotencyStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcIdempotencyStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code JdbcIdempotencyStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcIdempotencyStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcIdempotencyStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code JdbcIdempotencyStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code JdbcIdempotencyStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcIdempotencyStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scopeType 参数 scopeType；parameter scope type。
     * @param scopeId 参数 scopeId；parameter scope id。
     * @param key 参数 键；parameter key。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<Record> find(
            String scopeType,
            String scopeId,
            String key) {
        return jdbc.query("""
                SELECT scope_type, scope_id, idempotency_key, payload_sha256,
                       resource_id, response_content::text AS response_content,
                       created_at, expires_at
                  FROM gateway_idempotency_record
                 WHERE scope_type = ? AND scope_id = ?
                   AND idempotency_key = ?
                """, (result, row) -> new Record(
                result.getString("scope_type"),
                result.getString("scope_id"),
                result.getString("idempotency_key"),
                result.getString("payload_sha256"),
                result.getString("resource_id"),
                map(result.getString("response_content")),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("expires_at") == null
                        ? null
                        : result.getTimestamp("expires_at").toInstant()
        ), scopeType, scopeId, key).stream().findFirst();
    }

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcIdempotencyStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcIdempotencyStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcIdempotencyStore.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     */
    @Override
    public void save(Record record) {
        jdbc.update("""
                INSERT INTO gateway_idempotency_record(
                    scope_type, scope_id, idempotency_key, payload_sha256,
                    resource_id, response_content, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                """,
                record.scopeType(),
                record.scopeId(),
                record.key(),
                record.payloadSha256(),
                record.resourceId(),
                json(record.response()),
                timestamp(record.createdAt()),
                timestamp(record.expiresAt())
        );
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code JdbcIdempotencyStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code JdbcIdempotencyStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcIdempotencyStore.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "idempotency response cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcIdempotencyStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcIdempotencyStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcIdempotencyStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored idempotency response is invalid",
                    failure
            );
        }
    }
}
