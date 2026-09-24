package top.egon.cola.component.yuheng.admin.observability.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayAuditLogRecordPO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayAuditVO;

import java.util.List;

/**
 * gateway_audit_log 的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for gateway_audit_log; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用。
 */
public interface GatewayAuditLogDAO extends EgonColaMapper<GatewayAuditLogRecordPO> {

    /**
     * 中文说明：审计分页总数，与 {@link #selectAuditPage} 共用同一段可见性谓词（跨 {@code gateway_group} 的相关 EXISTS）。
     * English summary: Counts the audit page total, sharing one visibility predicate with {@link #selectAuditPage}
     * (the correlated EXISTS over {@code gateway_group}).
     *
     * 用法 / Usage: {@code env}/{@code namespace} 无条件参与 EXISTS，其余可选过滤项传 {@code null} 即不参与。
     * {@code env} and {@code namespace} always scope the EXISTS while every other filter applies only when present.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param actorId 操作者标识，可空；the optional actor identifier.
     * @param resourceId 资源标识，可空；the optional resource identifier.
     * @param traceId 链路标识，可空；the optional trace identifier.
     * @param successful 成功标志，可空；the optional success flag.
     * @return 活跃且租户可见的审计总数；the number of active, tenant-visible audit rows.
     */
    long countAudits(@Param("env") String env,
                     @Param("namespace") String namespace,
                     @Param("actorId") String actorId,
                     @Param("resourceId") String resourceId,
                     @Param("traceId") String traceId,
                     @Param("successful") Boolean successful);

    /**
     * 中文说明：按发生时间倒序读取一页审计行，替代旧 {@code SELECT id, actor_id, ... FROM gateway_audit_log a
     * ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset}，视图由门面按 {@link GatewayAuditLogRecordPO} 组装。
     * English summary: Reads one page of audit rows ordered by descending occurrence time, replacing the legacy
     * {@code SELECT id, actor_id, ... FROM gateway_audit_log a ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset},
     * while the facade assembles the views.
     *
     * 用法 / Usage: 与 {@link #countAudits} 传同一组取值，窗口按 {@code (page - 1) * size} 计算。
     * Pass the very same values as {@link #countAudits} with the window derived as {@code (page - 1) * size}.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param actorId 操作者标识，可空；the optional actor identifier.
     * @param resourceId 资源标识，可空；the optional resource identifier.
     * @param traceId 链路标识，可空；the optional trace identifier.
     * @param successful 成功标志，可空；the optional success flag.
     * @param limit 页大小；the page size.
     * @param offset 行偏移；the row offset.
     * @return 当前页的审计行；the audit rows of the requested page.
     */
    List<GatewayAuditVO> selectAuditPage(@Param("env") String env,
                                                  @Param("namespace") String namespace,
                                                  @Param("actorId") String actorId,
                                                  @Param("resourceId") String resourceId,
                                                  @Param("traceId") String traceId,
                                                  @Param("successful") Boolean successful,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);
}
