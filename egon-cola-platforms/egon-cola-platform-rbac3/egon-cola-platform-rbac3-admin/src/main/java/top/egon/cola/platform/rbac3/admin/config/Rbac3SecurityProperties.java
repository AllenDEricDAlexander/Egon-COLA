package top.egon.cola.platform.rbac3.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "egon.rbac3.security")
public class Rbac3SecurityProperties {

    private String jwtPrivateKeyFile;
    private String jwtPublicKeyFile;
    private String jwtKid;
    private String issuer;
    private List<String> audiences = new ArrayList<>();
    private String auditCursorSecretFile;
    private Duration verificationKeyRetention = Duration.ofDays(8);

    public String getJwtPrivateKeyFile() {
        return jwtPrivateKeyFile;
    }

    public void setJwtPrivateKeyFile(String jwtPrivateKeyFile) {
        this.jwtPrivateKeyFile = jwtPrivateKeyFile;
    }

    public String getJwtPublicKeyFile() {
        return jwtPublicKeyFile;
    }

    public void setJwtPublicKeyFile(String jwtPublicKeyFile) {
        this.jwtPublicKeyFile = jwtPublicKeyFile;
    }

    public String getJwtKid() {
        return jwtKid;
    }

    public void setJwtKid(String jwtKid) {
        this.jwtKid = jwtKid;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<String> getAudiences() {
        return List.copyOf(audiences);
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = new ArrayList<>(audiences == null ? List.of() : audiences);
    }

    public String getAuditCursorSecretFile() {
        return auditCursorSecretFile;
    }

    public void setAuditCursorSecretFile(String auditCursorSecretFile) {
        this.auditCursorSecretFile = auditCursorSecretFile;
    }

    public Duration getVerificationKeyRetention() {
        return verificationKeyRetention;
    }

    public void setVerificationKeyRetention(Duration verificationKeyRetention) {
        this.verificationKeyRetention = verificationKeyRetention;
    }

    public String requirePrivateKeyFile() {
        return required(jwtPrivateKeyFile, "jwtPrivateKeyFile");
    }

    public String requirePublicKeyFile() {
        return required(jwtPublicKeyFile, "jwtPublicKeyFile");
    }

    public String requireKid() {
        return required(jwtKid, "jwtKid");
    }

    public String requireIssuer() {
        return required(issuer, "issuer");
    }

    public List<String> requireAudiences() {
        List<String> values = audiences.stream()
                .map(value -> required(value, "audience"))
                .toList();
        if (values.isEmpty()) {
            throw new IllegalStateException("at least one JWT audience is required");
        }
        return values;
    }

    public String requireAuditCursorSecretFile() {
        return required(auditCursorSecretFile, "auditCursorSecretFile");
    }

    public Duration requireVerificationKeyRetention() {
        if (verificationKeyRetention == null || verificationKeyRetention.isNegative()
                || verificationKeyRetention.isZero()) {
            throw new IllegalStateException("verificationKeyRetention must be positive");
        }
        return verificationKeyRetention;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " is required");
        }
        return value.trim();
    }
}
