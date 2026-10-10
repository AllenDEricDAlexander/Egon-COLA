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

    @AssertTrue
    public boolean isReceiverValid() {
        return !enabled || fallbackIssuer != null && fallbackIssuer.isAbsolute()
                && "https".equalsIgnoreCase(fallbackIssuer.getScheme()) && fallbackIssuer.getHost() != null
                && fallbackIssuer.getUserInfo() == null && fallbackIssuer.getQuery() == null && fallbackIssuer.getFragment() == null;
    }
}
