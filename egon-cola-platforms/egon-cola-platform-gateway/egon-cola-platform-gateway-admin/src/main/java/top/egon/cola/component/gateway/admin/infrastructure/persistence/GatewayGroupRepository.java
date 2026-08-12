package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayGroupRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关GroupRepository相关的职责与边界。
 * English summary: {@code GatewayGroupRepository} is an interface contract in the current Gateway module; it owns the gateway group repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayGroupRepository
        extends JpaRepository<GatewayGroupEntity, String> {

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.findAllByDeletedFalseOrderByCreatedAtDesc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayGroupEntity> findAllByDeletedFalseOrderByCreatedAtDesc();

    /**
     * 中文说明：执行 findAllByEnvAnd命名空间AndDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by env and namespace and deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 findAllByEnvAnd命名空间AndDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayGroupEntity>
    findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
            String env,
            String namespace
    );

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by id and deleted false operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.findByIdAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayGroupEntity> findByIdAndDeletedFalse(String id);
}
