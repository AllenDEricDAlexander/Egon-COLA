package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.ToString;

/** Inputs for one frozen, explicitly reviewed identity/OAuth migration run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthMigrationCommand {

    @NotBlank
    @Pattern(regexp = "(?:platform|tenant:[1-9][0-9]{0,18})")
    private String realmKey;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._:-]{1,118}")
    private String sourceSnapshotId;

    private boolean dryRun;

    @NotNull
    private Path freezeMarkerFile;

    @NotNull
    private Path ownershipMapFile;

    @NotNull
    private Path checkpointFile;

    @NotNull
    private Path secretDistributionDirectory;

    @ToString.Exclude
    @Pattern(regexp = "(?:[0-9a-fA-F]{64})?")
    private String approvedReportSha256;

    @AssertTrue(message = "apply requires an approved dry-run report SHA-256")
    public boolean isApprovedReportForApply() {
        return dryRun || approvedReportSha256 != null && approvedReportSha256.matches("[0-9a-fA-F]{64}");
    }
}
