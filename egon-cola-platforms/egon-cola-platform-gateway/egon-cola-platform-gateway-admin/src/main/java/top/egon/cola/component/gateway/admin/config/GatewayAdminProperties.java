package top.egon.cola.component.gateway.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.admin")
public class GatewayAdminProperties {

    private RuleChunk ruleChunk = new RuleChunk();

    private Ddc ddc = new Ddc();

    public RuleChunk getRuleChunk() {
        return ruleChunk;
    }

    public void setRuleChunk(RuleChunk ruleChunk) {
        this.ruleChunk = ruleChunk;
    }

    public Ddc getDdc() {
        return ddc;
    }

    public void setDdc(Ddc ddc) {
        this.ddc = ddc;
    }

    public static class Ddc {

        private String targetBizCode = "infra";

        private String targetAppCode = "ge";

        public String getTargetBizCode() {
            return targetBizCode;
        }

        public void setTargetBizCode(String targetBizCode) {
            this.targetBizCode = targetBizCode;
        }

        public String getTargetAppCode() {
            return targetAppCode;
        }

        public void setTargetAppCode(String targetAppCode) {
            this.targetAppCode = targetAppCode;
        }
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
