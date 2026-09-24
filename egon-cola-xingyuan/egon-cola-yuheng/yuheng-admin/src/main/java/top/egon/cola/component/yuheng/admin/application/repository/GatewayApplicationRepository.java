package top.egon.cola.component.yuheng.admin.application.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.application.domain.bo.GatewayApplicationBO;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayApplicationRepository} 是 gateway_application 的业务端口，只声明当前调用方真正需要的
 * 命名方法，不继承 Spring Data/JPA 泛型 CRUD，也不外泄 MyBatis-Plus 行模型。
 * English summary: {@code GatewayApplicationRepository} is the business port for {@code gateway_application}; it declares
 * only the named methods current consumers need, inherits no Spring Data/JPA generic CRUD and leaks no MyBatis-Plus row model.
 *
 * 用法 / Usage: 由 {@code MpGatewayApplicationRepository} 在 {@code gatewayTransactionManager} 事务内以受守卫的
 * MP 写入实现；业务 {@code revision} 的乐观锁递增由仓储负责，不依赖托管实体脏检查。
 * {@code MpGatewayApplicationRepository} implements it through guarded MyBatis-Plus writes inside the
 * {@code gatewayTransactionManager} transaction, and the business {@code revision} increment is performed by the
 * repository rather than by managed-entity dirty checking.
 */
@Validated
public interface GatewayApplicationRepository {

    /**
     * 中文说明：保存应用载体并返回仓储侧权威值；新建写入 revision 0，更新按业务 revision 做 CAS 并递增，
     * 实现同时把权威 revision 与审计时间回写到入参载体，替代旧的托管实体脏检查。
     * English summary: Saves the application carrier and returns the repository-authoritative values; inserts store
     * revision 0 while updates compare-and-set on the business revision and increment it, and the implementation also
     * syncs the authoritative revision and audit timestamps back into the given carrier instead of relying on managed
     * entity dirty checking.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.save(application)}。
     * @param application 参数 应用载体；parameter the application carrier.
     * @return 返回 权威应用载体；returns the authoritative application carrier.
     */
    GatewayApplicationBO save(@Valid GatewayApplicationBO application);

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find all by deleted false order by created at desc operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 由 MP 仓储按 {@code create_time} 倒序返回活跃行，软删行由 {@code deleted_at} 逻辑删除条件排除。
     * The MP repository returns active rows ordered by {@code create_time} descending, soft-deleted rows being excluded
     * by the {@code deleted_at} logical-delete predicate.
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    List<GatewayApplicationBO> findAllByDeletedFalseOrderByCreatedAtDesc();

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by id and deleted false operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationRepository.findByIdAndDeletedFalse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayApplicationBO> findByIdAndDeletedFalse(@NotBlank String id);

    /**
     * 中文说明：执行 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 操作；该方法是 {@code GatewayApplicationRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find by biz code and application code and env and deleted false operation; this method is the invocation entry point on {@code GatewayApplicationRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 三元组 {@code bizCode}+{@code applicationCode}+{@code env} 与软删列 {@code deleted_at} 共同构成业务唯一键，
     * 因此该查询在活跃集合内至多一行。/ The {@code bizCode}, {@code applicationCode} and {@code env} columns combined with
     * the soft-delete column {@code deleted_at} form the business unique key, so at most one active row matches.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param env 参数 env；parameter env。
     * @return 返回 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    Optional<GatewayApplicationBO>
    findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
            @NotBlank String bizCode,
            @NotBlank String applicationCode,
            @NotBlank String env);
}
