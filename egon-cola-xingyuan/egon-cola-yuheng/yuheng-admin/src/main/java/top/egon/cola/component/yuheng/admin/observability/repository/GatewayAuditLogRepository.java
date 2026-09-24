package top.egon.cola.component.yuheng.admin.observability.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;

/**
 * 中文说明：{@code GatewayAuditLogRepository} 是管理面审计写入的业务端口，只声明调用方实际使用的写入契约，
 * 由受守卫的 MyBatis-Plus 实现承担租户、软删除与版本语义；不再继承 Spring Data，也不向调用方暴露托管实体。
 * English summary: {@code GatewayAuditLogRepository} is the business port for management audit writes; it declares only
 * the write contract callers use and its guarded MyBatis-Plus implementation owns tenancy, soft delete and versioning,
 * so no Spring Data inheritance or managed entity reaches a caller.
 *
 * 用法 / Usage: 通过 Spring 容器注入该端口类型使用；/ Inject the port type; {@code save} returns the persisted
 * business object including the identifiers and technical metadata the guard assigned.
 */
@Validated
public interface GatewayAuditLogRepository {

    /**
     * 中文说明：持久化一条审计记录并返回守卫补齐技术字段后的业务对象；新增与更新都由实现按主键是否存在决定。
     * English summary: Persists one audit record and returns the business object after the guard filled its technical
     * fields; the implementation decides insert versus update from the identifier.
     *
     * 用法 / Usage: {@code GatewayAuditLogRepository.save(auditLog)}。
     * @param auditLog 待写入的审计业务对象；the audit business object to persist。
     * @return 返回已持久化的审计业务对象；returns the persisted audit business object.
     */
    GatewayAuditLogBO save(@NotNull @Valid GatewayAuditLogBO auditLog);
}
