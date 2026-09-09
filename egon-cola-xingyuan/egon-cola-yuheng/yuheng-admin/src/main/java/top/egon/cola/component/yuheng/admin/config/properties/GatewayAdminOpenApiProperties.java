package top.egon.cola.component.yuheng.admin.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayOpenApiDnsPolicy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Typed limits and safety controls for the Gateway Admin OpenAPI reconciler.
 *
 * <p>中文：同步调度、网络超时、重试和文档上限集中在一个配置边界；配置
 * 绑定完成后由 {@link #validate()} 执行跨字段约束，避免同步服务自行维护
 * 不一致的默认值。</p>
 */
@Validated
@ConfigurationProperties(prefix = "gateway.admin.openapi")
public class GatewayAdminOpenApiProperties {

    private boolean enabled;

    private Duration reconcileDelay = Duration.ofSeconds(30);

    private int batchSize = 50;

    private int maximumInstanceAttempts = 3;

    private Duration claimTimeout = Duration.ofMinutes(2);

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    private int maximumDocumentBytes = 5 * 1024 * 1024;

    private int maximumOperations = 5_000;

    private int maximumSchemaNodes = 50_000;

    private int maximumRefDepth = 64;

    private Duration retryInitialDelay = Duration.ofSeconds(5);

    private Duration retryMaximumDelay = Duration.ofMinutes(5);

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double retryJitter = 0.20d;

    private Duration driftSampleInterval = Duration.ofMinutes(5);

    private List<String> allowedCidrs = new ArrayList<>();

    @NotBlank
    private String requiredScope = "gateway.openapi.read";

    /** Explicit local-only escape hatch; production remains HTTPS-only. */
    private boolean allowDevelopmentHttp;

    /**
     * Validates bounded scheduler and security settings after property
     * binding. Disabled OpenAPI sync may leave the CIDR list empty; an
     * enabled reconciler must fail closed without an allowlist.
     */
    public void validate() {
        requirePositive(reconcileDelay, "reconcile-delay");
        requireRange(batchSize, 1, 50, "batch-size");
        requireRange(
                maximumInstanceAttempts,
                1,
                3,
                "maximum-instance-attempts"
        );
        requirePositive(claimTimeout, "claim-timeout");
        requirePositive(connectTimeout, "connect-timeout");
        requirePositive(readTimeout, "read-timeout");
        requireRange(
                maximumDocumentBytes,
                1,
                5 * 1024 * 1024,
                "maximum-document-bytes"
        );
        requireRange(maximumOperations, 1, 5_000, "maximum-operations");
        requireRange(
                maximumSchemaNodes,
                1,
                50_000,
                "maximum-schema-nodes"
        );
        requireRange(maximumRefDepth, 1, 64, "maximum-ref-depth");
        requirePositive(retryInitialDelay, "retry-initial-delay");
        requirePositive(retryMaximumDelay, "retry-maximum-delay");
        if (retryMaximumDelay.compareTo(retryInitialDelay) < 0) {
            throw new IllegalArgumentException(
                    "retry-maximum-delay must not be shorter than "
                            + "retry-initial-delay"
            );
        }
        if (retryJitter < 0.0d || retryJitter > 1.0d) {
            throw new IllegalArgumentException(
                    "retry-jitter must be between 0 and 1"
            );
        }
        requirePositive(driftSampleInterval, "drift-sample-interval");
        String scope = requiredScope == null ? "" : requiredScope.trim();
        if (scope.isEmpty() || scope.length() > 256
                || scope.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "required-scope must be a non-blank bounded value"
            );
        }
        requiredScope = scope;
        allowedCidrs = normalizeCidrs(allowedCidrs);
        if (enabled && allowedCidrs.isEmpty()) {
            throw new IllegalArgumentException(
                    "allowed-cidrs must not be empty when OpenAPI sync is enabled"
            );
        }
        if (!allowedCidrs.isEmpty()) {
            // Reuse the existing CIDR parser/policy so binding and the HTTP
            // client cannot silently accept different network syntax.
            new GatewayOpenApiDnsPolicy(allowedCidrs);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getReconcileDelay() {
        return reconcileDelay;
    }

    public void setReconcileDelay(Duration reconcileDelay) {
        this.reconcileDelay = reconcileDelay;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaximumInstanceAttempts() {
        return maximumInstanceAttempts;
    }

    public void setMaximumInstanceAttempts(int maximumInstanceAttempts) {
        this.maximumInstanceAttempts = maximumInstanceAttempts;
    }

    public Duration getClaimTimeout() {
        return claimTimeout;
    }

    public void setClaimTimeout(Duration claimTimeout) {
        this.claimTimeout = claimTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaximumDocumentBytes() {
        return maximumDocumentBytes;
    }

    public void setMaximumDocumentBytes(int maximumDocumentBytes) {
        this.maximumDocumentBytes = maximumDocumentBytes;
    }

    public int getMaximumOperations() {
        return maximumOperations;
    }

    public void setMaximumOperations(int maximumOperations) {
        this.maximumOperations = maximumOperations;
    }

    public int getMaximumSchemaNodes() {
        return maximumSchemaNodes;
    }

    public void setMaximumSchemaNodes(int maximumSchemaNodes) {
        this.maximumSchemaNodes = maximumSchemaNodes;
    }

    public int getMaximumRefDepth() {
        return maximumRefDepth;
    }

    public void setMaximumRefDepth(int maximumRefDepth) {
        this.maximumRefDepth = maximumRefDepth;
    }

    public Duration getRetryInitialDelay() {
        return retryInitialDelay;
    }

    public void setRetryInitialDelay(Duration retryInitialDelay) {
        this.retryInitialDelay = retryInitialDelay;
    }

    public Duration getRetryMaximumDelay() {
        return retryMaximumDelay;
    }

    public void setRetryMaximumDelay(Duration retryMaximumDelay) {
        this.retryMaximumDelay = retryMaximumDelay;
    }

    public double getRetryJitter() {
        return retryJitter;
    }

    public void setRetryJitter(double retryJitter) {
        this.retryJitter = retryJitter;
    }

    public Duration getDriftSampleInterval() {
        return driftSampleInterval;
    }

    public void setDriftSampleInterval(Duration driftSampleInterval) {
        this.driftSampleInterval = driftSampleInterval;
    }

    public List<String> getAllowedCidrs() {
        return List.copyOf(allowedCidrs);
    }

    public void setAllowedCidrs(List<String> allowedCidrs) {
        this.allowedCidrs = allowedCidrs == null
                ? new ArrayList<>()
                : new ArrayList<>(allowedCidrs);
    }

    public String getRequiredScope() {
        return requiredScope;
    }

    public void setRequiredScope(String requiredScope) {
        this.requiredScope = requiredScope;
    }

    public boolean isAllowDevelopmentHttp() {
        return allowDevelopmentHttp;
    }

    public void setAllowDevelopmentHttp(boolean allowDevelopmentHttp) {
        this.allowDevelopmentHttp = allowDevelopmentHttp;
    }

    private static List<String> normalizeCidrs(List<String> values) {
        Objects.requireNonNull(values, "allowed-cidrs");
        return values.stream()
                .map(value -> Objects.requireNonNull(value, "allowed CIDR"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private static void requirePositive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireRange(
            int value,
            int minimum,
            int maximum,
            String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum
            );
        }
    }
}
