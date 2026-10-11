package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import jakarta.validation.constraints.AssertTrue;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;

/** Opt-in usage observation; anonymous failures use a configured trusted receiver issuer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Validated
@ConfigurationProperties("egon.cola.platform.tianquan.shoubing.usage-observation")
public class OAuthUsageObservationProperties {
    private boolean enabled;
    private URI fallbackIssuer;
    private boolean allowLocalPlaintext;
    private IdpSecurityObservationReceiverEnum receiver;

    /** Preserves the existing source/binary constructor; enabled deployments must configure a receiver. */
    public OAuthUsageObservationProperties(boolean enabled, URI fallbackIssuer, boolean allowLocalPlaintext) {
        this(enabled, fallbackIssuer, allowLocalPlaintext, null);
    }

    @AssertTrue
    public boolean isReceiverValid() {
        return !enabled || receiver != null && fallbackIssuer != null && fallbackIssuer.isAbsolute()
                && "https".equalsIgnoreCase(fallbackIssuer.getScheme()) && fallbackIssuer.getHost() != null
                && fallbackIssuer.getUserInfo() == null && fallbackIssuer.getQuery() == null && fallbackIssuer.getFragment() == null;
    }
}
