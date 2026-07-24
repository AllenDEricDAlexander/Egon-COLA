package ${package}.infrastructure.config.datasource;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Prevents the read/write overlay from being used without the sharding topology.
 */
@Configuration(proxyBeanMethods = false)
@Profile("readwrite")
public class ReadwriteProfileGuard {

    public ReadwriteProfileGuard(Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("sharding"))) {
            throw new IllegalStateException(
                    "the readwrite profile must be enabled together with sharding");
        }
    }
}
