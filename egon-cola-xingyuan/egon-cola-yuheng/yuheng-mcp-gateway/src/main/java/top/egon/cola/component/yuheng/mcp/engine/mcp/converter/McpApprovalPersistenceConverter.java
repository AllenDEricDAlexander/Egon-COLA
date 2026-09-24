package top.egon.cola.component.yuheng.mcp.engine.mcp.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentApprovalStatusEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpApprovalRecordPO;

import java.time.Instant;
import java.util.Objects;

/**
 * 中文说明：{@code McpApprovalPersistenceConverter} 是数据面审批消费路径上唯一的列映射入口：它只把读到的
 * {@code gateway_mcp_approval} 行投影成“一次性消费”所需的 CAS 行模型，写入 {@code status}/{@code consumed_at}/
 * 业务 {@code revision} 与 {@code updateTime}，并把技术 {@code id} 与读取到的 {@code version} 原样带上；
 * 数值 {@code tenant_id}、审计列与乐观锁 {@code version} 的推进只由受守卫边界补齐。数据面不签发、不撤销审批，
 * 因此本转换器没有 {@code newRow} 插入方向，也没有 {@code toBusiness} 反向投影：审批令牌摘要与调用方自报租户
 * 分别落在 {@code token_digest} 与 {@code subject_tenant_id}，前者绝不被重算，后者绝不与部署数值租户混用。
 * English summary: {@code McpApprovalPersistenceConverter} is the only column-mapping entry point on the data-plane
 * approval-consumption path: it projects a loaded {@code gateway_mcp_approval} row onto the row model the one-shot
 * compare-and-set needs, writing {@code status}, {@code consumed_at}, the business {@code revision} and
 * {@code updateTime} while carrying the technical {@code id} and the {@code version} that was read; the numeric
 * {@code tenant_id}, the audit columns and the advance of the optimistic-lock {@code version} are supplied exclusively
 * by the guarded boundary. Because the data plane neither issues nor revokes approvals this converter has no
 * {@code newRow} insert direction and no reverse {@code toBusiness} projection: the token digest lives in
 * {@code token_digest} and the caller-reported tenant in {@code subject_tenant_id}, the first never being recomputed
 * and the second never being mixed with the deployment-bound numeric tenant.
 *
 * 用法 / Usage: 由 {@code mcpApprovalAdapter} 注入使用，业务侧不得在其他位置手写审批列映射；本模块 pom 不含
 * {@code org.mapstruct}（新增依赖需单独批准），故本转换器为显式手写实现，映射语义与 MapStruct 生成的同名转换器
 * 一致，且与同包 {@code McpTaskPersistenceConverter} 保持同一形态（{@code @Slf4j} + 显式 Bean 名 +
 * {@code @RequiredArgsConstructor} + {@code @Validated}）。/ Inject it into {@code mcpApprovalAdapter} only, so no other
 * place hand-writes approval column mapping; MapStruct is not on this module's classpath (its pom has no
 * {@code org.mapstruct} entry and adding a dependency needs separate approval), which is why this converter is written
 * explicitly while keeping the semantics a generated converter of the same name would have, and it keeps the exact
 * shape of {@code McpTaskPersistenceConverter} in this package ({@code @Slf4j} plus an explicit bean name,
 * {@code @RequiredArgsConstructor} and {@code @Validated}).
 */
@Slf4j
@Component("mcpApprovalPersistenceConverter")
@RequiredArgsConstructor
@Validated
public class McpApprovalPersistenceConverter {

    /**
     * 中文说明：把读到的活跃审批行投影为一次性消费的 CAS 行模型：只下发 {@code status='CONSUMED'}、
     * {@code consumed_at}、自增后的业务 {@code revision} 与 {@code update_time} 四个非空列，
     * 外加 {@code id}+{@code version} 两个守卫条件；本语句不清空任何列，因此无需显式 {@code NULL} 写入。
     * English summary: Projects a loaded active approval row onto the compare-and-set row model of the one-shot
     * consumption: only the four non-null columns {@code status='CONSUMED'}, {@code consumed_at}, the incremented
     * business {@code revision} and {@code update_time} are pushed, beside the {@code id}+{@code version} guards, and no
     * column is cleared by this statement, so no explicit {@code NULL} write is required.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalPersistenceConverter.consumedRow(current, consumedAt)}。
     * @param current 参数 读到的活跃审批行；parameter the active row that was read.
     * @param consumedAt 参数 消费时刻；parameter the consumption instant.
     * @return 返回 待 CAS 的行模型；returns the row model to compare-and-set.
     */
    public McpApprovalRecordPO consumedRow(McpApprovalRecordPO current, Instant consumedAt) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(consumedAt, "consumedAt");
        return McpApprovalRecordPO.builder()
                .id(current.getId())
                .version(current.getVersion())
                .status(McpPersistentApprovalStatusEnum.CONSUMED)
                .consumedAt(consumedAt)
                .revision(nextRevision(current.getRevision()))
                .updateTime(consumedAt)
                .build();
    }

    /**
     * 中文说明：复核旧 {@code WHERE ... AND subject_id = ? AND tenant_id = ? AND client_id = ?} 等身份谓词：
     * 旧语句里的调用方租户即 {@code tenant_id} 列，迁移后该列是部署数值租户并由守卫过滤，调用方自报租户落在
     * {@code subject_tenant_id}，故按该列复核；任一列为空都按不匹配处理，绝不重算摘要。
     * English summary: Re-checks the legacy identity predicates
     * {@code WHERE ... AND subject_id = ? AND tenant_id = ? AND client_id = ?}: the caller tenant the legacy statement
     * compared against the {@code tenant_id} column is the deployment-bound numeric tenant after migration and is
     * enforced by the guard, while the caller-reported tenant now lives in {@code subject_tenant_id} and is therefore
     * re-checked on that column; an absent column counts as a mismatch and no digest is ever recomputed.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalPersistenceConverter.matchesIdentity(row, request)}。
     * @param row 参数 读到的活跃审批行；parameter the active row that was read.
     * @param request 参数 消费请求；parameter the consumption request.
     * @return 返回 身份六列是否全部匹配；returns whether all six identity columns match.
     */
    public boolean matchesIdentity(McpApprovalRecordPO row, McpApprovalPort.ConsumptionRequest request) {
        Objects.requireNonNull(row, "row");
        Objects.requireNonNull(request, "request");
        return Objects.equals(row.getSubjectId(), request.subjectId())
                && Objects.equals(row.getSubjectTenantId(), request.tenantId())
                && Objects.equals(row.getClientId(), request.clientId())
                && Objects.equals(row.getServerCode(), request.serverCode())
                && Objects.equals(row.getToolName(), request.toolName())
                && Objects.equals(row.getArgumentDigest(), request.argumentDigest());
    }

    /**
     * 中文说明：按旧 SQL 的 {@code revision = revision + 1} 递增业务修订，null 视为 0；MP 技术
     * {@code version} 由守卫另行推进，两者互不替代。
     * English summary: Increments the business revision, matching the legacy {@code revision = revision + 1} and treating
     * null as zero; the technical MP {@code version} is advanced separately by the guard and neither replaces the other.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalPersistenceConverter.nextRevision(revision)}。
     * @param revision 参数 读到的修订；parameter the stored revision.
     * @return 返回 自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(Long revision) {
        return (revision == null ? 0L : revision) + 1L;
    }
}
