package top.egon.cola.component.yuheng.admin.reporting.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiIngestionGroup;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.List;
import java.util.Objects;

/**
 * Transport-neutral command for writing one immutable Gateway Definition Set.
 *
 * <p>中文：HTTP OpenAPI 聚合和 RPC Descriptor 报告共用这个边界，但 snapshot
 * provenance 只属于 OpenAPI；sync row/revision 永远不穿透到此 command。</p>
 */
public record GatewayDefinitionIngestionCommandDTO(
        @NotBlank(groups = GatewayOpenApiIngestionGroup.class)
        @Size(max = 64, groups = GatewayOpenApiIngestionGroup.class)
        String applicationId,
        @NotNull(groups = GatewayOpenApiIngestionGroup.class)
        GatewayDefinitionSourceTypeEnum sourceType,
        @NotBlank(groups = GatewayOpenApiIngestionGroup.class)
        @Size(max = 256, groups = GatewayOpenApiIngestionGroup.class)
        String sourceScope,
        @Valid @NotNull(groups = GatewayOpenApiIngestionGroup.class)
        GatewayInterfaceDefinitionReport report,
        @NotNull(groups = GatewayOpenApiIngestionGroup.class)
        List<@NotBlank(groups = GatewayOpenApiIngestionGroup.class) String> snapshotIds
) {

    public GatewayDefinitionIngestionCommandDTO {
        applicationId = required(applicationId, "applicationId", 64);
        sourceType = Objects.requireNonNull(sourceType, "sourceType");
        sourceScope = required(sourceScope, "sourceScope", 256);
        report = Objects.requireNonNull(report, "report");
        snapshotIds = normalizeSnapshots(snapshotIds);
    }

    /** RPC reports deliberately carry no OpenAPI snapshot provenance. */
    public static GatewayDefinitionIngestionCommandDTO rpc(
            String applicationId,
            String sourceScope,
            GatewayInterfaceDefinitionReport report) {
        return new GatewayDefinitionIngestionCommandDTO(
                applicationId,
                GatewayDefinitionSourceTypeEnum.RPC_DESCRIPTOR,
                sourceScope,
                report,
                List.of()
        );
    }

    private static List<String> normalizeSnapshots(List<String> values) {
        Objects.requireNonNull(values, "snapshotIds");
        List<String> normalized = values.stream()
                .map(value -> required(value, "snapshotId", 64))
                .sorted()
                .toList();
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("snapshotIds must be unique");
        }
        return List.copyOf(normalized);
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " is blank or exceeds " + max + " characters"
            );
        }
        return normalized;
    }
}
