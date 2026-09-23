package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpServerBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpServerBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpServerBO {

    private String id;
    private String gatewayGroupId;
    private String serverCode;
    private String displayName;
    private String description;
    private String instructions;
    private Set<String> dialects;
    private String resourceUri;
    private long listCacheTtlSeconds;
    private boolean enabled;
    private long revision;
    private boolean deleted;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public void update(
                String displayName,
                String description,
                String instructions,
                Set<String> dialects,
                String resourceUri,
                long listCacheTtlSeconds,
                boolean enabled,
                long expectedRevision,
                AdminActor actor,
                Instant now) {
            assertRevision(expectedRevision);
            this.displayName = required(displayName, "displayName");
            this.description = optional(description);
            this.instructions = optional(instructions);
            this.dialects = nonEmpty(dialects, "dialects");
            this.resourceUri = resourceUri(resourceUri);
            this.listCacheTtlSeconds = nonNegative(
                    listCacheTtlSeconds,
                    "listCacheTtlSeconds"
            );
            this.enabled = enabled;
            updatedAt = now;
            updatedBy = actor(actor);
        }

    public void softDelete(
                long expectedRevision,
                AdminActor actor,
                Instant now) {
            assertRevision(expectedRevision);
            deleted = true;
            enabled = false;
            updatedAt = now;
            updatedBy = actor(actor);
        }

    public void assertRevision(long expectedRevision) {
            if (revision != expectedRevision) {
                throw new GatewayAdminRevisionConflictException(revision);
            }
        }

    public Set<String> getDialects() {
            return Set.copyOf(dialects);
        }

    private static String actor(AdminActor actor) {
            return java.util.Objects.requireNonNull(actor, "actor").actorId();
        }

    private static Set<String> nonEmpty(Set<String> values, String field) {
            Set<String> copy = Set.copyOf(values == null ? Set.of() : values);
            if (copy.isEmpty()) {
                throw new IllegalArgumentException(field + " must not be empty");
            }
            return copy;
        }

    private static long nonNegative(long value, String field) {
            if (value < 0) {
                throw new IllegalArgumentException(field + " must not be negative");
            }
            return value;
        }

    private static String optional(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }

    private static String resourceUri(String value) {
            URI uri;
            try {
                uri = URI.create(required(value, "resourceUri")).normalize();
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException(
                        "resourceUri must be a valid URI", invalid);
            }
            if (!uri.isAbsolute() || uri.getFragment() != null) {
                throw new IllegalArgumentException(
                        "resourceUri must be absolute and must not contain a fragment"
                );
            }
            return uri.toString();
        }

    private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }

    /**
         * 中文说明：创建 {@code McpServerBO} 实例并保留原持久化载体的构造语义（业务 revision、审计时间与缺省状态）。
         * English summary: Creates a {@code McpServerBO} while preserving the construction semantics of the legacy carrier (business revision, audit timestamps and default state).
         *
         * 用法 / Usage: 由服务层在建立新业务对象时调用；/ Call it from the service layer when a new business object is created.
         */
        public McpServerBO(
                String id,
                String gatewayGroupId,
                String serverCode,
                String displayName,
                String description,
                String instructions,
                Set<String> dialects,
                String resourceUri,
                long listCacheTtlSeconds,
                AdminActor actor,
                Instant now) {
            this.id = required(id, "id");
            this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
            this.serverCode = required(serverCode, "serverCode");
            this.displayName = required(displayName, "displayName");
            this.description = optional(description);
            this.instructions = optional(instructions);
            this.dialects = nonEmpty(dialects, "dialects");
            this.resourceUri = resourceUri(resourceUri);
            this.listCacheTtlSeconds = nonNegative(
                    listCacheTtlSeconds,
                    "listCacheTtlSeconds"
            );
            enabled = true;
            createdAt = now;
            updatedAt = now;
            createdBy = actor(actor);
            updatedBy = createdBy;
        }
}
