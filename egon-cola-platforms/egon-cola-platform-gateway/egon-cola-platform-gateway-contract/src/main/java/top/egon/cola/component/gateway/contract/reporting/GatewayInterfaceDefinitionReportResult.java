package top.egon.cola.component.gateway.contract.reporting;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 接口定义报告提交后的处理结果。
 *
 * <p>除了总体状态，还返回导入统计、操作身份映射以及可定位到报告路径的警告。
 */
public record GatewayInterfaceDefinitionReportResult(
        String reportId,
        String definitionSetId,
        Status status,
        String applicationId,
        Counts counts,
        List<OperationRef> operationRefs,
        List<Warning> warnings,
        Instant receivedAt
) {

    public GatewayInterfaceDefinitionReportResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(counts, "counts");
        operationRefs = List.copyOf(operationRefs);
        warnings = List.copyOf(warnings);
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    /**
     * 报告接收状态。
     */
    public enum Status {
        ACCEPTED,
        ACCEPTED_WITH_WARNINGS,
        REJECTED
    }

    /**
     * 本次报告在目录中创建、更新和缺失对象的数量统计。
     */
    public record Counts(
            int businessDomains,
            int entityDomains,
            int interfaceGroups,
            int operations,
            int created,
            int updated,
            int missingFromThisSet
    ) {
    }

    /**
     * 报告中的操作键与 Gateway 持久化操作 ID 的映射。
     */
    public record OperationRef(
            String operationKey,
            String operationId,
            String changeType
    ) {
    }

    /**
     * 报告处理时发现的非致命问题及其字段路径。
     */
    public record Warning(
            String path,
            String code,
            String message
    ) {
    }
}
