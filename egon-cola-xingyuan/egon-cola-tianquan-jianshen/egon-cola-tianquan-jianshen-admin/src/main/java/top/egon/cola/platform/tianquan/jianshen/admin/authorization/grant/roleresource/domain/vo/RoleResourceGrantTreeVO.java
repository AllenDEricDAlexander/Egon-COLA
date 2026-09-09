package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Read-only resource catalog/grant projection for role configuration. */
@Schema(description = "角色资源目录与直接授权、派生接口的只读投影")
public record RoleResourceGrantTreeVO(
        @Schema(description = "角色 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String roleId,
        @Schema(description = "角色所属的全局应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @Schema(description = "角色版本；保存授权时作为 expectedRoleVersion 提交", minimum = "0")
        long roleVersion,
        @Schema(description = "角色直接绑定的资源 ID；派生接口另见 derivedApiResourceIds")
        List<String> directResourceIds,
        @Schema(description = "由直接绑定的页面或按钮关联关系派生的接口资源 ID")
        List<String> derivedApiResourceIds,
        @Schema(description = "资源目录与当前授权的统计信息")
        Summary summary,
        @Schema(description = "按父子关系组织的资源树根节点集合")
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

    @Schema(name = "RoleResourceGrantSummary", description = "角色资源授权统计")
    public record Summary(
            @Schema(description = "目录中的页面资源数量（ROUTE）", minimum = "0") long menuPageCount,
            @Schema(description = "目录中的按钮或操作资源数量（ACTION）", minimum = "0") long actionCount,
            @Schema(description = "直接授权与派生接口去重后的资源总数，包含页面和按钮", minimum = "0") long apiCount,
            @Schema(description = "继承授权资源数量；当前投影未计算时为 0", minimum = "0") long inheritedCount) {
        public Summary {
            if (menuPageCount < 0L || actionCount < 0L || apiCount < 0L
                    || inheritedCount < 0L) {
                throw new IllegalArgumentException("summary counts must not be negative");
            }
        }
    }

    @Schema(name = "RoleResourceGrantNode", description = "资源树节点及该角色对资源的授权状态")
    public record Node(
            @Schema(description = "资源 ID")
            String resourceId,
            @Schema(description = "应用内稳定的资源编码")
            String resourceCode,
            @Schema(description = "资源显示名称")
            String name,
            @Schema(description = "资源展示分类，例如 MENU_PAGE、ACTION 或 API")
            String category,
            @Schema(description = "资源技术类型，例如 MENU、ROUTE、ACTION 或 API")
            String technicalType,
            @Schema(description = "父资源编码；根节点为空", types = {"string", "null"})
            String parentCode,
            @Schema(description = "资源生命周期状态")
            String status,
            @Schema(description = "资源与实际权限字符的映射配置状态")
            String mappingStatus,
            @Schema(description = "角色授权状态，例如 DIRECT、DERIVED 或 NONE")
            String grantState,
            @Schema(description = "该资源当前是否满足直接授权条件")
            boolean grantable,
            @Schema(description = "不可直接授权的原因码；满足授权条件时为空", types = {"string", "null"})
            String disabledReason,
            @Schema(description = "页面或按钮关联的接口元数据")
            List<LinkedApi> linkedApis,
            @Schema(description = "直接子资源节点；叶子节点为空数组")
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

    @Schema(name = "RoleResourceLinkedApi", description = "与页面或按钮关联的接口")
    public record LinkedApi(
            @Schema(description = "接口资源 ID") String resourceId,
            @Schema(description = "接口资源编码") String resourceCode,
            @Schema(description = "接口显示名称") String name,
            @Schema(description = "HTTP 方法；未提供时为空", types = {"string", "null"}) String method,
            @Schema(description = "HTTP 请求路径；未提供时为空", types = {"string", "null"}) String path) {
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
