package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class OAuthRealmBO {

    @Positive
    private Long id;
    @PositiveOrZero
    private long version;
    @NotNull
    private URI issuer;
    @NotBlank
    @Pattern(regexp = "platform|tenant:[1-9][0-9]{0,18}")
    @Size(max = 64)
    private String realmKey;
    @NotNull
    private RealmTypeEnum realmType;
    @Positive
    private Long businessTenantId;
    @NotNull
    @Positive
    private Long storagePartitionId;
    @NotBlank
    @Pattern(regexp = "INITIALIZING|ACTIVE|SUSPENDED|CLOSED")
    @Size(max = 24)
    private String status;
    @Size(max = 128)
    private String activeKid;
    @NotNull
    @PositiveOrZero
    private Long trustVersion;
}
