package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

/** Immutable configuration value for durable, secret-free OAuth audit delivery. */
@Validated
@Qualifier("oauthAuditProperties")
@ConfigurationProperties(prefix = "egon.cola.platform.tianquan.shoubing.audit")
public record OAuthAuditProperties(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:._-]{1,128}") String stream,
        @NotNull Path spoolDirectory,
        @Positive @Max(1_073_741_824L) long spoolMaxBytes,
        @NotNull Path hmacKeyFile) {

    private static final String DEFAULT_STREAM = "egon:oauth:audit:v1";
    private static final long DEFAULT_SPOOL_MAX_BYTES = 1_073_741_824L;

    public OAuthAuditProperties {
        stream = stream == null ? DEFAULT_STREAM : stream;
        spoolMaxBytes = spoolMaxBytes == 0L ? DEFAULT_SPOOL_MAX_BYTES : spoolMaxBytes;
        if (spoolDirectory != null && !spoolDirectory.isAbsolute()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_SPOOL_DIRECTORY_MUST_BE_ABSOLUTE");
        }
        if (hmacKeyFile != null && !hmacKeyFile.isAbsolute()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_HMAC_KEY_FILE_MUST_BE_ABSOLUTE");
        }
    }

    @Override
    public String toString() {
        return "OAuthAuditProperties[stream=" + stream + ", spoolConfigured=" + (spoolDirectory != null)
                + ", spoolMaxBytes=" + spoolMaxBytes + ", hmacKeyConfigured=" + (hmacKeyFile != null) + "]";
    }
}
