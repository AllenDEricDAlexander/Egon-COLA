package top.egon.cola.component.yuheng.admin.group.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.group.domain.bo.GatewayGroupBO;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayGroupRepository} 是 gateway_group 的业务端口，只声明当前调用方真正需要的命名方法，
 * 不继承 Spring Data/JPA 泛型 CRUD，也不外泄 MyBatis-Plus 行模型。
 * English summary: {@code GatewayGroupRepository} is the business port for {@code gateway_group}; it declares only the
 * named methods current consumers need, inherits no Spring Data/JPA generic CRUD and leaks no MyBatis-Plus row model.
 *
 * 用法 / Usage: 由 {@code MpGatewayGroupRepository} 以受守卫的 MP 写入实现，保存返回权威业务载体，
 * 业务 {@code revision} 的 CAS 与递增在仓储内完成，替代旧的托管实体脏检查。
 * {@code MpGatewayGroupRepository} implements it through guarded MyBatis-Plus writes, returning the authoritative
 * carrier and performing the business {@code revision} compare-and-set plus increment in the repository instead of the
 * former managed-entity dirty checking.
 */
@Validated
public interface GatewayGroupRepository {

    /**
     * 中文说明：保存分组载体并返回仓储侧权威值；新建写入 revision 0，更新按业务 revision 做 CAS 并递增，
     * 同时把权威 revision 与审计时间回写入参载体。
     * English summary: Saves the group carrier and returns the repository-authoritative values; inserts store revision 0
     * while updates compare-and-set on the business revision and increment it, syncing the authoritative revision and
     * audit timestamps back into the given carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.save(group)}。
     * @param group 参数 分组载体；parameter the group carrier.
     * @return 返回 权威分组载体；returns the authoritative group carrier.
     */
    GatewayGroupBO save(@Valid GatewayGroupBO group);

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 由 MP 仓储按 {@code create_time} 倒序返回活跃行，软删行由 {@code deleted_at} 逻辑删除条件排除。
     * The MP repository returns active rows ordered by {@code create_time} descending, soft-deleted rows being excluded
     * by the {@code deleted_at} logical-delete predicate.
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayGroupBO> findAllByDeletedFalseOrderByCreatedAtDesc();

    /**
     * 中文说明：执行 findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by env and namespace and deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 findAllByEnvAnd命名空间AndDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayGroupBO>
    findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
            @NotBlank String env,
            @NotBlank String namespace
    );

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；该方法是 {@code GatewayGroupRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by id and deleted false operation; this method is the invocation entry point on {@code GatewayGroupRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupRepository.findByIdAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayGroupBO> findByIdAndDeletedFalse(@NotBlank String id);
}
