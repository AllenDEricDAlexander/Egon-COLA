package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;

import java.net.URI;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class TrustedRealmBO {

    @NotNull
    private URI issuer;
    @NotBlank
    @Size(max = 64)
    private String realmKey;
    @NotNull
    private RealmTypeEnum realmType;
    @NotNull
    @Positive
    private Long storagePartitionId;
    @Positive
    private Long businessTenantId;
    @NotNull
    @PositiveOrZero
    private Long trustVersion;

    public void validateTrustedPartition() {
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(realmType, "realmType");
        if (!issuer.isAbsolute() || !"https".equalsIgnoreCase(issuer.getScheme())
                || issuer.getHost() == null || issuer.getUserInfo() != null
                || issuer.getQuery() != null || issuer.getFragment() != null
                || issuer.toASCIIString().length() > 2048) {
            throw new IllegalArgumentException("TRUSTED_REALM_ISSUER_INVALID");
        }
        if (realmKey == null || realmKey.isBlank() || !realmKey.equals(realmKey.trim())
                || realmKey.length() > 64) {
            throw new IllegalArgumentException("TRUSTED_REALM_KEY_INVALID");
        }
        if (storagePartitionId == null || storagePartitionId <= 0L
                || trustVersion == null || trustVersion < 0L) {
            throw new IllegalArgumentException("TRUSTED_REALM_PARTITION_INVALID");
        }
        if (realmType == RealmTypeEnum.TENANT
                ? !Objects.equals(businessTenantId, storagePartitionId)
                        || !Objects.equals(realmKey, "tenant:" + businessTenantId)
                : businessTenantId != null || !"platform".equals(realmKey)) {
            throw new IllegalArgumentException("TRUSTED_REALM_BUSINESS_PARTITION_MISMATCH");
        }
    }
}
