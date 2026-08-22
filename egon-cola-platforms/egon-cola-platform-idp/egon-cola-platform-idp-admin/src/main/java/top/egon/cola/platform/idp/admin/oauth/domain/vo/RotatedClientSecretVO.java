package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import java.time.Instant;

/** One-time response returned after an administrator rotates a Client Secret. */
public record RotatedClientSecretVO(
        String clientId,
        String appId,
        String clientSecret,
        String secretHint,
        long version,
        Instant rotatedAt
) {

    @Override
    public String toString() {
        return "RotatedClientSecretVO[clientId=" + clientId
                + ", appId=" + appId
                + ", clientSecret=<redacted>"
                + ", secretHint=" + secretHint
                + ", version=" + version
                + ", rotatedAt=" + rotatedAt
                + ']';
    }
}
