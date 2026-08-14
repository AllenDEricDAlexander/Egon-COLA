package top.egon.cola.component.gateway.admin.application.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayApplicationRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关ApplicationRepository相关的职责与边界。
 * English summary: {@code GatewayApplicationRepository} is an interface contract in the current Gateway module; it owns the gateway application repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayApplicationRepository
        extends JpaRepository<GatewayApplicationPO, String> {

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.findAllByDeletedFalseOrderByCreatedAtDesc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayApplicationPO> findAllByDeletedFalseOrderByCreatedAtDesc();

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by id and deleted false operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.findByIdAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayApplicationPO> findByIdAndDeletedFalse(String id);

    /**
     * 中文说明：执行 findByApplicationCodeAndEnvAnd命名空间AndDeletedFalse 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by application code and env and namespace and deleted false operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.findByApplicationCodeAndEnvAndNamespaceAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 findByApplicationCodeAndEnvAnd命名空间AndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayApplicationPO>
    findByApplicationCodeAndEnvAndNamespaceAndDeletedFalse(
            String applicationCode,
            String env,
            String namespace);

    /**
     * 中文说明：执行 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by biz code and application code and env and deleted false operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param env 参数 env；parameter env。
     * @return 返回 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayApplicationPO>
    findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
            String bizCode,
            String applicationCode,
            String env);
}
