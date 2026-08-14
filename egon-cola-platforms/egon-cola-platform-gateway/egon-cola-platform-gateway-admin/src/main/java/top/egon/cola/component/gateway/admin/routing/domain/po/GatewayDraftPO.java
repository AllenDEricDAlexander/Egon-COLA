package top.egon.cola.component.gateway.admin.routing.domain.po;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayDraftPO} 是类型，位于当前 Gateway 模块的相关包中，负责网关草稿Entity相关的职责与边界。
 * English summary: {@code GatewayDraftPO} is a type in the current Gateway module; it owns the gateway draft entity-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Entity
@Table(name = "gateway_draft")
public class GatewayDraftPO {

    /**
     * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Id
    @Column(name = "gateway_group_id")
    private String gatewayGroupId;

    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Version
    @Column(nullable = false)
    private long revision;

    /**
     * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "based_on_release_id")
    private String basedOnReleaseId;

    /**
     * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private String status;

    /**
     * 中文说明：保存 changeSummary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by change summary; its type is {@code String}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "change_summary")
    private String changeSummary;

    /**
     * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code GatewayDraftPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /**
     * 中文说明：创建 {@code GatewayDraftPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    protected GatewayDraftPO() {
    }

    /**
     * 中文说明：创建 {@code GatewayDraftPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public GatewayDraftPO(
            String gatewayGroupId,
            String actor,
            Instant now) {
        this.gatewayGroupId = gatewayGroupId;
        status = "EDITABLE";
        updatedBy = actor;
        updatedAt = now;
    }

    /**
     * 中文说明：执行 assertEditable 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the assert editable operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.assertEditable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    public void assertEditable(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new top.egon.cola.component.gateway.admin.shared.domain.exception
                    .GatewayAdminRevisionConflictException(revision);
        }
        if (!"EDITABLE".equals(status)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_DRAFT_NOT_EDITABLE"
            );
        }
    }

    /**
     * 中文说明：执行 touch 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the touch operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.touch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param reason 参数 reason；parameter reason。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void touch(String reason, String actor, Instant now) {
        changeSummary = reason;
        updatedBy = actor;
        updatedAt = now;
    }

    /**
     * 中文说明：执行 changeStatus 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the change status operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.changeStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void changeStatus(String status, String actor, Instant now) {
        this.status = status;
        updatedBy = actor;
        updatedAt = now;
    }

    /**
     * 中文说明：执行 baseOn 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the base on operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.baseOn(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     */
    public void baseOn(String releaseId, String actor, Instant now) {
        basedOnReleaseId = releaseId;
        touch("published " + releaseId, actor, now);
    }

    /**
     * 中文说明：执行 get网关GroupId 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get gateway group id operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getGatewayGroupId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get网关GroupId 的处理结果；returns the result of the operation.
     */
    public String getGatewayGroupId() {
        return gatewayGroupId;
    }

    /**
     * 中文说明：执行 getRevision 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get revision operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRevision 的处理结果；returns the result of the operation.
     */
    public long getRevision() {
        return revision;
    }

    /**
     * 中文说明：执行 getBasedOn发布Id 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get based on release id operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getBasedOnReleaseId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getBasedOn发布Id 的处理结果；returns the result of the operation.
     */
    public String getBasedOnReleaseId() {
        return basedOnReleaseId;
    }

    /**
     * 中文说明：执行 getStatus 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get status operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getStatus 的处理结果；returns the result of the operation.
     */
    public String getStatus() {
        return status;
    }

    /**
     * 中文说明：执行 getChangeSummary 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get change summary operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getChangeSummary(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getChangeSummary 的处理结果；returns the result of the operation.
     */
    public String getChangeSummary() {
        return changeSummary;
    }

    /**
     * 中文说明：执行 getUpdatedAt 操作；该方法是 {@code GatewayDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get updated at operation; this method is the invocation entry point on {@code GatewayDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftPO.getUpdatedAt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getUpdatedAt 的处理结果；returns the result of the operation.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
