package top.egon.cola.component.yuheng.admin.reporting.repository;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.reporting.domain.bo.GatewayStoredReportBO;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;

import java.time.Instant;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayDefinitionReportRepository} 是网关接口定义上报（Definition Set）的写读端口：按调用方的真实用量声明
 * 构建指纹查询、定义集存在性查询、starter 操作计数与一次完整定义集上报入库五项契约；签名只使用契约记录与领域 BO，
 * 不出现 MyBatis-Plus 行模型、JPA 派生查询名或 SQL 片段，租户过滤、活跃读取、乐观锁 CAS 与 jsonb 绑定全部由
 * {@code repository/impl} 下的门面实现经各表受守卫持久化边界承担。
 * English summary: {@code GatewayDefinitionReportRepository} is the gateway interface-definition report (Definition Set) port: it
 * declares the five contracts callers actually use — build-fingerprint lookup, definition-set existence, starter operation counting
 * and one complete definition-set ingestion. Its signatures carry only contract records and domain BOs, never MyBatis-Plus row
 * models, Spring Data derived-query names or SQL fragments, while tenant filtering, active reads, optimistic-lock CAS and jsonb
 * binding stay inside the facade implementation under {@code repository/impl} and its guarded per-table persistence boundaries.
 *
 * 用法 / Usage: 由 {@code GatewayDefinitionReportService} 与 {@code GatewayDefinitionIngestionService} 通过本端口注入，
 * 上报写入必须留在调用方的事务边界内；实现不新增异常类型，冲突仍抛原有的
 * {@code IllegalStateException} 文本码。/ Inject it through this port in {@code GatewayDefinitionReportService} and
 * {@code GatewayDefinitionIngestionService}; ingestion writes stay inside the caller's transaction, and the implementation adds no
 * exception type — conflicts keep raising the pre-existing textual {@code IllegalStateException} codes.
 */
@Validated
public interface GatewayDefinitionReportRepository {

    /**
     * 中文说明：执行 findBuildFingerprint 操作；返回该应用同一 {@code buildId} 已登记的不可变定义指纹，
     * 与原 {@code SELECT DISTINCT fingerprint FROM gateway_definition_set WHERE application_id = ? AND build_id = ?} 一致：
     * 无记录时为空，存在多个指纹时按指纹升序取首个，以获得可重复的判定结果。
     * English summary: Executes the findBuildFingerprint operation; returns the immutable definition fingerprint already registered for
     * this application and {@code buildId}, matching {@code SELECT DISTINCT fingerprint FROM gateway_definition_set WHERE application_id
     * = ? AND build_id = ?}: empty when nothing is registered and, when several fingerprints exist, the ascending first one so the
     * verdict stays repeatable.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.findBuildFingerprint(applicationId, buildId)}。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param buildId 参数 构建 id；parameter build id。
     * @return 返回构建指纹或空；returns the build fingerprint or empty.
     */
    Optional<String> findBuildFingerprint(
            @NotBlank String applicationId,
            @NotBlank String buildId);

    /**
     * 中文说明：执行 findBuildFingerprint 操作；在应用与构建之外再按 {@code protocol} 限定指纹作用域，等价于原
     * {@code SELECT DISTINCT fingerprint ... AND protocol = ?}，用于 HTTP 聚合上报与 RPC 上报相互隔离的不变式判定。
     * 形参 {@code sourceScope} 属于上报来源作用域，原 SQL 未把它作为过滤条件，本端口沿用同一语义，只在契约中保留入参形状。
     * English summary: Executes the findBuildFingerprint operation scoped by {@code protocol} in addition to application and build, equal
     * to the original {@code SELECT DISTINCT fingerprint ... AND protocol = ?}, which keeps the HTTP aggregation and RPC reporting
     * immutability verdicts apart. The {@code sourceScope} parameter names the reporting source scope, which the original SQL never
     * filtered on; this port preserves that behavior and keeps the parameter only for the caller's input shape.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.findBuildFingerprint(applicationId, buildId, protocol,
     * sourceScope)}；实现必须真正按协议过滤，不得退回两参查询。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param buildId 参数 构建 id；parameter build id。
     * @param protocol 参数 协议原 wire 字符串；parameter protocol wire string。
     * @param sourceScope 参数 上报来源作用域，不参与过滤；parameter reporting source scope, not a filter.
     * @return 返回构建指纹或空；returns the build fingerprint or empty.
     */
    Optional<String> findBuildFingerprint(
            @NotBlank String applicationId,
            @NotBlank String buildId,
            @NotBlank String protocol,
            @NotBlank String sourceScope);

    /**
     * 中文说明：执行 definitionSetExists 操作；判断该应用是否已登记指定定义集，等价于原
     * {@code SELECT COUNT(*) ... WHERE application_id = ? AND id = ?} 的正则化，用于重复上报时跳过入库；
     * 无法定位数值主键的标识如实返回不存在。
     * English summary: Executes the definitionSetExists operation; a regularized form of the original
     * {@code SELECT COUNT(*) ... WHERE application_id = ? AND id = ?} used to skip re-ingestion of a reported set, and an identifier
     * that cannot address a numeric primary key is reported truthfully as absent.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.definitionSetExists(applicationId, definitionSetId)}。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param definitionSetId 参数 定义集 id；parameter definition set id。
     * @return 返回定义集是否存在；returns whether the definition set exists.
     */
    boolean definitionSetExists(
            @NotBlank String applicationId,
            @NotBlank String definitionSetId);

    /**
     * 中文说明：执行 countStarterOperations 操作；统计该应用由 RPC 描述符来源拥有的操作数量，等价于原
     * {@code SELECT COUNT(*) FROM gateway_operation WHERE application_id = ? AND source_type = 'RPC_DESCRIPTOR'}，
     * 调用方据此推算本次上报后消失的旧操作数。
     * English summary: Executes the countStarterOperations operation; counts the operations this application owns through the RPC
     * descriptor source, equal to the original {@code SELECT COUNT(*) FROM gateway_operation WHERE application_id = ? AND source_type
     * = 'RPC_DESCRIPTOR'}, which lets the caller derive how many previously reported operations the current report no longer covers.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.countStarterOperations(applicationId)}。
     * @param applicationId 参数 应用 id；parameter application id.
     * @return 返回操作数量，无匹配为 0；returns the operation count, zero when nothing matches.
     */
    int countStarterOperations(@NotBlank String applicationId);

    /**
     * 中文说明：执行 ingest 操作；落库一个完整且已验证的定义集——写入定义集行（状态 {@code VERIFIED}、
     * {@code receivedAt} 与 {@code completedAt} 均为本次时间），沿业务域→实体域→接口分组→操作逐级查找或创建，
     * 并为每个操作追加定义版本、写入定义集成员关系以及在允许时把操作指向新定义；返回新增/更新计数与逐操作变更引用。
     * 来源冲突沿用原有 {@code IllegalStateException} 文本码，0 行写入不会被记为成功。
     * English summary: Executes the ingest operation; persists one complete, verified definition set — inserting the definition-set row
     * (status {@code VERIFIED} with both {@code receivedAt} and {@code completedAt} set to this instant), walking
     * business domain → entity domain → interface group → operation while finding or creating each level, then appending a definition
     * version per operation, recording the definition-set membership and pointing the operation at the new definition where allowed.
     * Source conflicts keep the pre-existing textual {@code IllegalStateException} codes and a zero-row write is never counted as
     * success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.ingest(applicationId, report, now)}；
     * 需在调用方事务内执行，返回值仅承载本次上报的创建/更新事实。
     * @param applicationId 参数 应用 id；parameter application id。
     * @param report 参数 完整定义集报告；parameter the complete definition-set report。
     * @param now 参数 本次上报时间；parameter this reporting instant。
     * @return 返回已入库上报载体；returns the stored-report carrier.
     */
    GatewayStoredReportBO ingest(
            @NotBlank String applicationId,
            @NotNull GatewayInterfaceDefinitionReport report,
            @NotNull Instant now);
}
