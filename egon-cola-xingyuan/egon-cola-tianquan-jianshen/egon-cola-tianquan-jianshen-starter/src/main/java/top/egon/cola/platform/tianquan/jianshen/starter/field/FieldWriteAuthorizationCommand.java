package top.egon.cola.platform.tianquan.jianshen.starter.field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Server-owned submitted field presence; an explicitly submitted null is still a write. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class FieldWriteAuthorizationCommand {
    public interface WriteFields { }

    @NotBlank(groups = WriteFields.class)
    @Size(max = 128, groups = WriteFields.class)
    @Pattern(regexp = "[A-Za-z0-9._:-]+", groups = WriteFields.class)
    private String permissionCode;

    @NotBlank(groups = WriteFields.class)
    @Size(max = 128, groups = WriteFields.class)
    @Pattern(regexp = "[A-Za-z0-9._:-]+", groups = WriteFields.class)
    private String applicationCode;

    @NotBlank(groups = WriteFields.class)
    @Size(max = 128, groups = WriteFields.class)
    @Pattern(regexp = "[A-Za-z0-9._:-]+", groups = WriteFields.class)
    private String resourceCode;

    @NotEmpty(groups = WriteFields.class)
    @Size(max = 100, groups = WriteFields.class)
    private Set<@NotBlank(groups = WriteFields.class) @Size(max = 128, groups = WriteFields.class)
            @Pattern(regexp = "[A-Za-z0-9._:-]+", groups = WriteFields.class) String> submittedFields;

    public Set<String> getSubmittedFields() {
        return submittedFields == null ? null : Set.copyOf(submittedFields);
    }

    public FieldWriteAuthorizationCommand setSubmittedFields(Set<String> fields) {
        this.submittedFields = fields == null ? null : Set.copyOf(fields);
        return this;
    }

    public static class FieldWriteAuthorizationCommandBuilder {
        public FieldWriteAuthorizationCommandBuilder submittedFields(Set<String> fields) {
            this.submittedFields = fields == null ? null : Set.copyOf(fields);
            return this;
        }
    }
}
