package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo;

import java.util.List;
import java.util.Objects;

/** Read-only resource catalog/grant projection for role configuration. */
public record RoleResourceGrantTreeVO(
        String roleId,
        String applicationId,
        long roleVersion,
        List<String> directResourceIds,
        List<String> derivedApiResourceIds,
        Summary summary,
        List<Node> nodes) {

    public RoleResourceGrantTreeVO {
        roleId = required(roleId, "roleId");
        applicationId = required(applicationId, "applicationId");
        if (roleVersion < 0L) {
            throw new IllegalArgumentException("roleVersion must not be negative");
        }
        directResourceIds = sorted(directResourceIds);
        derivedApiResourceIds = sorted(derivedApiResourceIds);
        summary = Objects.requireNonNull(summary, "summary");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
    }

    public record Summary(long menuPageCount, long actionCount,
                          long apiCount, long inheritedCount) {
        public Summary {
            if (menuPageCount < 0L || actionCount < 0L || apiCount < 0L
                    || inheritedCount < 0L) {
                throw new IllegalArgumentException("summary counts must not be negative");
            }
        }
    }

    public record Node(
            String resourceId,
            String resourceCode,
            String name,
            String category,
            String technicalType,
            String parentCode,
            String status,
            String mappingStatus,
            String grantState,
            boolean grantable,
            String disabledReason,
            List<LinkedApi> linkedApis,
            List<Node> children) {
        public Node {
            resourceId = required(resourceId, "resourceId");
            resourceCode = required(resourceCode, "resourceCode");
            name = required(name, "name");
            category = required(category, "category");
            technicalType = required(technicalType, "technicalType");
            status = required(status, "status");
            mappingStatus = required(mappingStatus, "mappingStatus");
            grantState = required(grantState, "grantState");
            linkedApis = List.copyOf(Objects.requireNonNull(linkedApis, "linkedApis"));
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }
    }

    public record LinkedApi(String resourceId, String resourceCode,
                            String name, String method, String path) {
        public LinkedApi {
            resourceId = required(resourceId, "resourceId");
            resourceCode = required(resourceCode, "resourceCode");
            name = required(name, "name");
        }
    }

    private static List<String> sorted(List<String> values) {
        return values == null ? List.of() : values.stream().sorted().toList();
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
