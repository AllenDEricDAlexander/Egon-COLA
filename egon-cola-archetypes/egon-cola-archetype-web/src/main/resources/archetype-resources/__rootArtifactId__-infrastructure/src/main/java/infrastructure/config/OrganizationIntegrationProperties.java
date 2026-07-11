package ${package}.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("organizationIntegrationProperties")
@ConfigurationProperties(prefix = "organization.integrations")
public class OrganizationIntegrationProperties {
    private Duration userTtl = Duration.ofMinutes(10);
    private Duration gradeTtl = Duration.ofMinutes(30);
    private Duration schoolClassTtl = Duration.ofMinutes(10);
    private Duration commandIdempotencyTtl = Duration.ofHours(24);

    public Duration getUserTtl() { return userTtl; }
    public void setUserTtl(Duration userTtl) { this.userTtl = userTtl; }
    public Duration getGradeTtl() { return gradeTtl; }
    public void setGradeTtl(Duration gradeTtl) { this.gradeTtl = gradeTtl; }
    public Duration getSchoolClassTtl() { return schoolClassTtl; }
    public void setSchoolClassTtl(Duration schoolClassTtl) { this.schoolClassTtl = schoolClassTtl; }
    public Duration getCommandIdempotencyTtl() { return commandIdempotencyTtl; }
    public void setCommandIdempotencyTtl(Duration ttl) { this.commandIdempotencyTtl = ttl; }
}
