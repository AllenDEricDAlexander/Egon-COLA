package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Secret-free summary produced by dry-run and apply migration executions. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthMigrationReportResult {

    private String realmKey;
    private String sourceSnapshotId;
    private boolean dryRun;
    private String sourceFingerprint;
    private String ownershipMapFingerprint;
    private String reportSha256;
    private String checkpointId;
    private long sourceCount;
    private long importedCount;
    private long skippedCount;
    private long conflictCount;
    private List<String> conflictCodes;
    private long confidentialClientSecretFilesPending;
    private List<String> secretFileReferences;
    private long legacySigningKeysRequireReplacement;
}
