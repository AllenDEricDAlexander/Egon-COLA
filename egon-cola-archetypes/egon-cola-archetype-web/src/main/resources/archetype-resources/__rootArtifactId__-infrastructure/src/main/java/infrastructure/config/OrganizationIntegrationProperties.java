package ${package}.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("organizationIntegrationProperties")
@ConfigurationProperties(prefix = "organization")
public class OrganizationIntegrationProperties {
    private final Cache cache = new Cache();
    private final Command command = new Command();

    public Cache getCache() { return cache; }
    public Command getCommand() { return command; }
    public Duration getUserTtl() { return cache.userTtl; }
    public Duration getGradeTtl() { return cache.gradeTtl; }
    public Duration getSchoolClassTtl() { return cache.schoolClassTtl; }
    public Duration getCommandIdempotencyTtl() { return command.idempotencyTtl; }

    public static final class Cache {
        private Duration userTtl = Duration.ofMinutes(10);
        private Duration gradeTtl = Duration.ofMinutes(30);
        private Duration schoolClassTtl = Duration.ofMinutes(10);

        public Duration getUserTtl() { return userTtl; }
        public void setUserTtl(Duration userTtl) { this.userTtl = userTtl; }
        public Duration getGradeTtl() { return gradeTtl; }
        public void setGradeTtl(Duration gradeTtl) { this.gradeTtl = gradeTtl; }
        public Duration getSchoolClassTtl() { return schoolClassTtl; }
        public void setSchoolClassTtl(Duration schoolClassTtl) { this.schoolClassTtl = schoolClassTtl; }
    }

    public static final class Command {
        private Duration idempotencyTtl = Duration.ofHours(24);

        public Duration getIdempotencyTtl() { return idempotencyTtl; }
        public void setIdempotencyTtl(Duration idempotencyTtl) { this.idempotencyTtl = idempotencyTtl; }
    }
}
