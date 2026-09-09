package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Untrusted HTTP body for replacing a role's complete direct resource set. */
@Schema(description = "原子替换角色完整的直接授权资源集合")
public record ReplaceRoleResourcesRequestDTO(
        @ArraySchema(arraySchema = @Schema(description = "新的完整资源 ID 集合；空数组表示清空直接授权",
                requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}"),
                maxItems = 2000, uniqueItems = true)
        List<String> resourceIds,
        @Schema(description = "授权生效时间；为空时使用服务端当前时间", types = {"string", "null"}, format = "date-time")
        Instant validFrom,
        @Schema(description = "授权结束时间；为空表示不设结束时间，非空时必须晚于生效时间",
                types = {"string", "null"}, format = "date-time")
        Instant validTo,
        @Schema(description = "读取资源树时获得的角色版本，用于防止覆盖并发修改", minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expectedRoleVersion) {

    public ReplaceRoleResourcesRequestDTO {
        resourceIds = List.copyOf(Objects.requireNonNull(resourceIds, "resourceIds"));
        if (resourceIds.size() > 2000) {
            throw new IllegalArgumentException("resourceIds must contain at most 2000 items");
        }
        if (new HashSet<>(resourceIds).size() != resourceIds.size()) {
            throw new IllegalArgumentException("resourceIds must be unique");
        }
        resourceIds = resourceIds.stream().map(value -> positiveDecimal(value, "resourceId"))
                .toList();
        if (validTo != null && validFrom != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        if (expectedRoleVersion < 0L) {
            throw new IllegalArgumentException("expectedRoleVersion must not be negative");
        }
    }

    private static String positiveDecimal(String value, String fieldName) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(fieldName + " must be a positive decimal id");
        }
        return value;
    }
}
