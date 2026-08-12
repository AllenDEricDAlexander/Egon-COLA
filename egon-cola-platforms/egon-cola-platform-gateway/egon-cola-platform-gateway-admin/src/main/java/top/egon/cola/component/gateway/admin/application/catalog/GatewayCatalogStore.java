package top.egon.cola.component.gateway.admin.application.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayCatalogStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关目录存储相关的职责与边界。
 * English summary: {@code GatewayCatalogStore} is an interface contract in the current Gateway module; it owns the gateway catalog store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayCatalogStore {

    /**
     * 中文说明：执行 load目录 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load catalog operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.loadCatalog(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 load目录 的处理结果；returns the result of the operation.
     */
    CatalogTree loadCatalog(String applicationId);

    /**
     * 中文说明：执行 createManualHierarchy 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create manual hierarchy operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.createManualHierarchy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param now 参数 now；parameter now。
     * @return 返回 createManualHierarchy 的处理结果；returns the result of the operation.
     */
    String createManualHierarchy(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now);

    /**
     * 中文说明：执行 find接口Group 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find interface group operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.findInterfaceGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @return 返回 find接口Group 的处理结果；returns the result of the operation.
     */
    Optional<InterfaceGroupScope> findInterfaceGroup(String interfaceGroupId);

    /**
     * 中文说明：执行 find操作 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.findOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 find操作 的处理结果；returns the result of the operation.
     */
    Optional<OperationRecord> findOperation(String operationId);

    /**
     * 中文说明：执行 find操作 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.findOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @return 返回 find操作 的处理结果；returns the result of the operation.
     */
    Optional<OperationRecord> findOperation(
            String applicationId,
            String operationKey);

    /**
     * 中文说明：执行 loadDefinitions 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load definitions operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.loadDefinitions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 loadDefinitions 的处理结果；returns the result of the operation.
     */
    List<OperationDefinition> loadDefinitions(String operationId);

    /**
     * 中文说明：执行 loadCurrent操作Definitions 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load current operation definitions operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.loadCurrentOperationDefinitions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 loadCurrent操作Definitions 的处理结果；returns the result of the operation.
     */
    List<CurrentOperationDefinition> loadCurrentOperationDefinitions(
            String gatewayGroupId);

    /**
     * 中文说明：执行 insert操作 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.insertOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     */
    void insertOperation(OperationRecord operation);

    /**
     * 中文说明：执行 append定义 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append definition operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.appendDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param definition 参数 定义；parameter definition。
     */
    void appendDefinition(OperationDefinition definition);

    /**
     * 中文说明：执行 pointTo定义 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the point to definition operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.pointToDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionId 参数 定义Id；parameter definition id。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param now 参数 now；parameter now。
     */
    void pointToDefinition(
            String operationId,
            String definitionId,
            boolean externalAccessible,
            Instant now);

    /**
     * 中文说明：执行 deprecate 操作；该方法是 {@code GatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deprecate operation; this method is the invocation entry point on {@code GatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogStore.deprecate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param now 参数 now；parameter now。
     */
    void deprecate(String operationId, Instant now);

    /**
     * 中文说明：{@code ManualHierarchy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ManualHierarchy相关的职责与边界。
     * English summary: {@code ManualHierarchy} is an immutable data carrier in the current Gateway module; it owns the manual hierarchy-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param businessCode 参数 businessCode；parameter business code。
     * @param businessName 参数 businessName；parameter business name。
     * @param entityCode 参数 entityCode；parameter entity code。
     * @param entityName 参数 entityName；parameter entity name。
     * @param interfaceGroupCode 参数 接口GroupCode；parameter interface group code。
     * @param interfaceGroupName 参数 接口GroupName；parameter interface group name。
     * @param className 参数 className；parameter class name。
     * @param description 参数 description；parameter description。
     */
    record ManualHierarchy(
            /**
             * 中文说明：保存 businessCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by business code; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String businessCode,
            /**
             * 中文说明：保存 businessName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by business name; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String businessName,
            /**
             * 中文说明：保存 entityCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entity code; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String entityCode,
            /**
             * 中文说明：保存 entityName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entity name; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String entityName,
            /**
             * 中文说明：保存 接口GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group code; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String interfaceGroupCode,
            /**
             * 中文说明：保存 接口GroupName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group name; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String interfaceGroupName,
            /**
             * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String className,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.ManualHierarchy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayCatalogStore.ManualHierarchy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.ManualHierarchy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.ManualHierarchy}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description
    ) {
    }

    /**
     * 中文说明：{@code InterfaceGroupScope} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责接口GroupScope相关的职责与边界。
     * English summary: {@code InterfaceGroupScope} is an immutable data carrier in the current Gateway module; it owns the interface group scope-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     */
    record InterfaceGroupScope(
            /**
             * 中文说明：保存 接口GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group id; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String interfaceGroupId,
            /**
             * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationId,
            /**
             * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String bizCode,
            /**
             * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationCode,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupScope} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupScope} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupScope} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupScope}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace
    ) {
    }

    /**
     * 中文说明：{@code OperationRecord} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Record相关的职责与边界。
     * English summary: {@code OperationRecord} is an immutable data carrier in the current Gateway module; it owns the operation record-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @param protocol 参数 protocol；parameter protocol。
     * @param methodIdentity 参数 方法身份；parameter method identity。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param providerServiceIdentity 参数 提供方服务身份；parameter provider service identity。
     * @param sourceType 参数 sourceType；parameter source type。
     * @param lifecycleStatus 参数 生命周期Status；parameter lifecycle status。
     * @param currentDefinitionId 参数 current定义Id；parameter current definition id。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    record OperationRecord(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationId,
            /**
             * 中文说明：保存 接口GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group id; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String interfaceGroupId,
            /**
             * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationKey,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 方法身份 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method identity; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String methodIdentity,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible,
            /**
             * 中文说明：保存 提供方服务身份 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider service identity; its type is {@code Map<String, Object>}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> providerServiceIdentity,
            /**
             * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceType,
            /**
             * 中文说明：保存 生命周期Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lifecycle status; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String lifecycleStatus,
            /**
             * 中文说明：保存 current定义Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by current definition id; its type is {@code String}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            String currentDefinitionId,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCatalogStore.OperationRecord} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayCatalogStore.OperationRecord} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationRecord} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationRecord}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 中文说明：{@code OperationDefinition} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作定义相关的职责与边界。
     * English summary: {@code OperationDefinition} is an immutable data carrier in the current Gateway module; it owns the operation definition-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionVersion 参数 定义Version；parameter definition version。
     * @param definitionSha256 参数 定义Sha256；parameter definition sha256。
     * @param summary 参数 summary；parameter summary。
     * @param tags 参数 tags；parameter tags。
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @param responseSchema 参数 响应模式；parameter response schema。
     * @param errorSchema 参数 error模式；parameter error schema。
     * @param descriptorSnapshot 参数 descriptorSnapshot；parameter descriptor snapshot。
     * @param attributes 参数 attributes；parameter attributes。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param createdBy 参数 createdBy；parameter created by。
     */
    record OperationDefinition(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 定义Version 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition version; its type is {@code long}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            long definitionVersion,
            /**
             * 中文说明：保存 定义Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition sha256; its type is {@code String}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String definitionSha256,
            /**
             * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String summary,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<String> tags,
            /**
             * 中文说明：保存 请求模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> requestSchema,
            /**
             * 中文说明：保存 响应模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by response schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> responseSchema,
            /**
             * 中文说明：保存 error模式 对应的状态、依赖或配置值；字段类型为 {@code List<Map<String, Object>>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error schema; its type is {@code List<Map<String, Object>>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<Map<String, Object>> errorSchema,
            /**
             * 中文说明：保存 descriptorSnapshot 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by descriptor snapshot; its type is {@code Map<String, Object>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> descriptorSnapshot,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> attributes,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code GatewayCatalogStore.OperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String createdBy
    ) {
    }

    /**
     * 中文说明：{@code CurrentOperationDefinition} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Current操作定义相关的职责与边界。
     * English summary: {@code CurrentOperationDefinition} is an immutable data carrier in the current Gateway module; it owns the current operation definition-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param operation 参数 操作；parameter operation。
     * @param definition 参数 定义；parameter definition。
     */
    record CurrentOperationDefinition(
            /**
             * 中文说明：保存 操作 对应的状态、依赖或配置值；字段类型为 {@code OperationRecord}，由 {@code GatewayCatalogStore.CurrentOperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation; its type is {@code OperationRecord}, and {@code GatewayCatalogStore.CurrentOperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.CurrentOperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.CurrentOperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            OperationRecord operation,
            /**
             * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code OperationDefinition}，由 {@code GatewayCatalogStore.CurrentOperationDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code OperationDefinition}, and {@code GatewayCatalogStore.CurrentOperationDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.CurrentOperationDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.CurrentOperationDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            OperationDefinition definition
    ) {
    }

    /**
     * 中文说明：{@code CatalogTree} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责目录Tree相关的职责与边界。
     * English summary: {@code CatalogTree} is an immutable data carrier in the current Gateway module; it owns the catalog tree-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param businessDomains 参数 businessDomains；parameter business domains。
     */
    record CatalogTree(
            /**
             * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.CatalogTree} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code GatewayCatalogStore.CatalogTree} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.CatalogTree} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.CatalogTree}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationId,
            /**
             * 中文说明：保存 businessDomains 对应的状态、依赖或配置值；字段类型为 {@code List<BusinessNode>}，由 {@code GatewayCatalogStore.CatalogTree} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by business domains; its type is {@code List<BusinessNode>}, and {@code GatewayCatalogStore.CatalogTree} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.CatalogTree} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.CatalogTree}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<BusinessNode> businessDomains
    ) {
    }

    /**
     * 中文说明：{@code BusinessNode} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责BusinessNode相关的职责与边界。
     * English summary: {@code BusinessNode} is an immutable data carrier in the current Gateway module; it owns the business node-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param code 参数 code；parameter code。
     * @param displayName 参数 displayName；parameter display name。
     * @param entityDomains 参数 entityDomains；parameter entity domains。
     */
    record BusinessNode(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.BusinessNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.BusinessNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.BusinessNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.BusinessNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.BusinessNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayCatalogStore.BusinessNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.BusinessNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.BusinessNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String code,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.BusinessNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayCatalogStore.BusinessNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.BusinessNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.BusinessNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 entityDomains 对应的状态、依赖或配置值；字段类型为 {@code List<EntityNode>}，由 {@code GatewayCatalogStore.BusinessNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entity domains; its type is {@code List<EntityNode>}, and {@code GatewayCatalogStore.BusinessNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.BusinessNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.BusinessNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<EntityNode> entityDomains
    ) {
    }

    /**
     * 中文说明：{@code EntityNode} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责EntityNode相关的职责与边界。
     * English summary: {@code EntityNode} is an immutable data carrier in the current Gateway module; it owns the entity node-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param code 参数 code；parameter code。
     * @param displayName 参数 displayName；parameter display name。
     * @param interfaceGroups 参数 接口Groups；parameter interface groups。
     */
    record EntityNode(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.EntityNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.EntityNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.EntityNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.EntityNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.EntityNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayCatalogStore.EntityNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.EntityNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.EntityNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String code,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.EntityNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayCatalogStore.EntityNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.EntityNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.EntityNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 接口Groups 对应的状态、依赖或配置值；字段类型为 {@code List<InterfaceGroupNode>}，由 {@code GatewayCatalogStore.EntityNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface groups; its type is {@code List<InterfaceGroupNode>}, and {@code GatewayCatalogStore.EntityNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.EntityNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.EntityNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<InterfaceGroupNode> interfaceGroups
    ) {
    }

    /**
     * 中文说明：{@code InterfaceGroupNode} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责接口GroupNode相关的职责与边界。
     * English summary: {@code InterfaceGroupNode} is an immutable data carrier in the current Gateway module; it owns the interface group node-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param code 参数 code；parameter code。
     * @param displayName 参数 displayName；parameter display name。
     * @param sourceType 参数 sourceType；parameter source type。
     * @param className 参数 className；parameter class name。
     * @param operations 参数 operations；parameter operations。
     */
    record InterfaceGroupNode(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String code,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceType,
            /**
             * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String className,
            /**
             * 中文说明：保存 operations 对应的状态、依赖或配置值；字段类型为 {@code List<OperationNode>}，由 {@code GatewayCatalogStore.InterfaceGroupNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operations; its type is {@code List<OperationNode>}, and {@code GatewayCatalogStore.InterfaceGroupNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.InterfaceGroupNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.InterfaceGroupNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<OperationNode> operations
    ) {
    }

    /**
     * 中文说明：{@code OperationNode} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Node相关的职责与边界。
     * English summary: {@code OperationNode} is an immutable data carrier in the current Gateway module; it owns the operation node-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @param protocol 参数 protocol；parameter protocol。
     * @param methodIdentity 参数 方法身份；parameter method identity。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param lifecycleStatus 参数 生命周期Status；parameter lifecycle status。
     * @param sourceType 参数 sourceType；parameter source type。
     * @param revision 参数 revision；parameter revision。
     */
    record OperationNode(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationKey,
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String protocol,
            /**
             * 中文说明：保存 方法身份 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method identity; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String methodIdentity,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible,
            /**
             * 中文说明：保存 生命周期Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by lifecycle status; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String lifecycleStatus,
            /**
             * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceType,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCatalogStore.OperationNode} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayCatalogStore.OperationNode} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogStore.OperationNode} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogStore.OperationNode}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {
    }
}
