package top.egon.cola.platform.tianquan.shoubing.admin.token.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Qualifier("oauthKeyEncryptionProperties")
@ConfigurationProperties(prefix = "egon.tianquan-shoubing.oauth.key-encryption")
@Validated
public class OAuthKeyEncryptionProperties {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    private String keyId;

    @NotNull
    @ToString.Exclude
    private Path keyFile;
}
