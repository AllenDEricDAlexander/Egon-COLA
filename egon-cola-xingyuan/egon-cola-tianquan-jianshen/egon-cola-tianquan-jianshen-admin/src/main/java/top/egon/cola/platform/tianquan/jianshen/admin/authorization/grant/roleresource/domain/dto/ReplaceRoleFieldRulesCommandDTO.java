package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.enums.FieldRuleStatusEnum;

/** Explicit operator replacement; empty desired rules revoke the role's active field rules. */
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class ReplaceRoleFieldRulesCommandDTO {
    public interface Mutation { }
    @NotNull(groups = Mutation.class) @Positive(groups = Mutation.class) private Long tenantId;
    @NotNull(groups = Mutation.class) @Positive(groups = Mutation.class) private Long applicationId;
    @NotNull(groups = Mutation.class) @Positive(groups = Mutation.class) private Long roleId;
    @NotNull(groups = Mutation.class) @Min(value = 0, groups = Mutation.class) private Long expectedRoleVersion;
    @NotBlank(groups = Mutation.class) @Size(max = 128, groups = Mutation.class)
    @Pattern(regexp = "[\\x21-\\x7E]{1,128}", groups = Mutation.class) private String actorId;
    @NotNull(groups = Mutation.class) @Size(max = 256, groups = Mutation.class) @Valid
    private List<@NotNull(groups = Mutation.class) RoleFieldRuleDTO> rules;

    @JsonIgnore @AssertTrue(groups = Mutation.class, message = "Role field rules must not repeat permission, field and start time")
    public boolean isRuleSetValid() {
        return rules != null && rules.stream().noneMatch(Objects::isNull)
                && rules.stream().map(rule -> java.util.Arrays.asList(rule.getPermissionId(), rule.getFieldDefinitionId(), rule.getValidFrom()))
                        .distinct().count() == rules.size();
    }

    public ReplaceRoleFieldRulesCommandDTO setRules(List<RoleFieldRuleDTO> values) {
        rules = values == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(values)); return this;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class RoleFieldRuleDTO {
        @NotNull(groups = Mutation.class) @Positive(groups = Mutation.class) private Long permissionId;
        @NotNull(groups = Mutation.class) @Positive(groups = Mutation.class) private Long fieldDefinitionId;
        @NotNull(groups = Mutation.class) private FieldAccessLevel accessLevel;
        @NotNull(groups = Mutation.class) private Instant validFrom;
        private Instant validTo;
        @NotNull(groups = Mutation.class) private FieldRuleStatusEnum status;

        @JsonIgnore @AssertTrue(groups = Mutation.class, message = "Rule end time must be later than start time")
        public boolean isWindowValid() { return validFrom != null && (validTo == null || validTo.isAfter(validFrom)); }
    }
}
