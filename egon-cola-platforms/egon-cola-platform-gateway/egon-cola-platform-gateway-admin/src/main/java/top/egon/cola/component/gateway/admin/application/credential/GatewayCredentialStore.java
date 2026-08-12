package top.egon.cola.component.gateway.admin.application.credential;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayCredentialStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关凭证存储相关的职责与边界。
 * English summary: {@code GatewayCredentialStore} is an interface contract in the current Gateway module; it owns the gateway credential store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayCredentialStore {

    /**
     * 中文说明：执行 insert 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.insert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param credential 参数 凭证；parameter credential。
     */
    void insert(CredentialRecord credential);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<CredentialRecord> find(String applicationId, String keyId);

    /**
     * 中文说明：执行 findByAccess键 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by access key operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.findByAccessKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessKey 参数 access键；parameter access key。
     * @return 返回 findByAccess键 的处理结果；returns the result of the operation.
     */
    Optional<CredentialRecord> findByAccessKey(String accessKey);

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    List<CredentialRecord> list(String applicationId);

    /**
     * 中文说明：执行 overlap 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the overlap operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.overlap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param validUntil 参数 validUntil；parameter valid until。
     * @param now 参数 now；parameter now。
     */
    void overlap(String id, Instant validUntil, Instant now);

    /**
     * 中文说明：执行 revoke 操作；该方法是 {@code GatewayCredentialStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke operation; this method is the invocation entry point on {@code GatewayCredentialStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCredentialStore.revoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param now 参数 now；parameter now。
     */
    void revoke(String id, Instant now);

    /**
     * 中文说明：{@code CredentialRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责凭证Record相关的职责与边界。
     * English summary: {@code CredentialRecord} is an immutable data carrier in the current Gateway module; it owns the credential record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param accessKey 参数 access键；parameter access key。
     * @param secretCiphertext 参数 secretCiphertext；parameter secret ciphertext。
     * @param keyVersion 参数 键Version；parameter key version。
     * @param status 参数 status；parameter status。
     * @param validFrom 参数 validFrom；parameter valid from。
     * @param validUntil 参数 validUntil；parameter valid until。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    record CredentialRecord(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationId,
            /**
             * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String accessKey,
            /**
             * 中文说明：保存 secretCiphertext 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by secret ciphertext; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String secretCiphertext,
            /**
             * 中文说明：保存 键Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by key version; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String keyVersion,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 validFrom 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid from; its type is {@code Instant}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validFrom,
            /**
             * 中文说明：保存 validUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid until; its type is {@code Instant}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant validUntil,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCredentialStore.CredentialRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayCredentialStore.CredentialRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCredentialStore.CredentialRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCredentialStore.CredentialRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }
}
