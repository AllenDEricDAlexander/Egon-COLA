package top.egon.cola.component.gateway.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.admin")
public class GatewayAdminProperties {

    private RuleChunk ruleChunk = new RuleChunk();

    public RuleChunk getRuleChunk() {
        return ruleChunk;
    }

    public void setRuleChunk(RuleChunk ruleChunk) {
        this.ruleChunk = ruleChunk;
    }

    public static class RuleChunk {

        private Duration retention = Duration.ofHours(24);

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }
    }
}
