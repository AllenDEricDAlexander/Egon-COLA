package top.egon.cola.component.yuheng.admin.openapi.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Durable per-Group OpenAPI synchronization states.
 *
 * <p>中文：OpenAPI Group 同步持久状态及其允许的 CAS 状态迁移，原 wire 字符串保持不变。
 */
public enum GatewayOpenApiSyncStateEnum implements EgonEnum {

    /** A provider Group was discovered but not claimed. */
    DISCOVERED(10, "DISCOVERED", "已发现"),

    /** A worker owns a fetch attempt. */
    FETCHING(20, "FETCHING", "抓取中"),

    /** The bounded document is being validated and canonicalized. */
    VALIDATING(30, "VALIDATING", "校验中"),

    /** The document is retained but cannot be ingested. */
    INVALID(40, "INVALID", "文档无效"),

    /** Same-build manifest or contract drift was detected. */
    INCONSISTENT_BUILD(50, "INCONSISTENT_BUILD", "构建不一致"),

    /** Definition ingestion is in progress or awaiting post-commit CAS. */
    INGESTING(60, "INGESTING", "摄取中"),

    /** The Group document and aggregate Definition Set are current. */
    VALID(70, "VALID", "已生效"),

    /** A definition transaction failed and may be retried. */
    INGEST_FAILED(80, "INGEST_FAILED", "摄取失败"),

    /** A provider fetch failed and may be retried. */
    FETCH_FAILED(90, "FETCH_FAILED", "抓取失败"),

    /** The provider observation is no longer healthy/current. */
    STALE(100, "STALE", "已过期");

    private static final Map<String, GatewayOpenApiSyncStateEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(GatewayOpenApiSyncStateEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    GatewayOpenApiSyncStateEnum(int code, String wireValue, String message) {
        this.code = code;
        this.wireValue = wireValue;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * 中文说明：按原 wire 字符串在不可变查表中精确匹配，未知值与 null 一律拒绝而非静默兜底。
     * English summary: Looks the value up in the immutable wire map and rejects unknown or null input instead of coercing it.
     */
    @JsonCreator
    public static GatewayOpenApiSyncStateEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("GatewayOpenApiSyncStateEnum wire value is required");
        }
        GatewayOpenApiSyncStateEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown GatewayOpenApiSyncStateEnum wire value: " + wireValue);
        }
        return matched;
    }

    /**
     * Returns whether a state transition is legal for the durable state
     * machine.
     *
     * @param target requested next state
     * @return {@code true} when the transition is allowed
     */
    public boolean canTransitionTo(GatewayOpenApiSyncStateEnum target) {
        Objects.requireNonNull(target, "target");
        return switch (this) {
            case DISCOVERED -> target == FETCHING || target == STALE;
            case FETCHING -> target == VALIDATING
                    || target == FETCH_FAILED
                    || target == STALE;
            case VALIDATING -> target == INVALID
                    || target == INCONSISTENT_BUILD
                    || target == INGESTING
                    || target == STALE;
            case INVALID -> target == STALE;
            case INCONSISTENT_BUILD -> target == STALE;
            case INGESTING -> target == VALID
                    || target == INGEST_FAILED
                    || target == STALE;
            case VALID -> target == STALE;
            case INGEST_FAILED -> target == DISCOVERED
                    || target == FETCHING
                    || target == STALE;
            case FETCH_FAILED -> target == DISCOVERED
                    || target == FETCHING
                    || target == STALE;
            case STALE -> target == DISCOVERED || target == FETCHING;
        };
    }

    /**
     * Returns whether a state can be retried without a new build.
     *
     * @return {@code true} for transient fetch/ingestion failures
     */
    public boolean retryable() {
        return this == FETCH_FAILED || this == INGEST_FAILED;
    }

    /**
     * Returns whether a row may be claimed at the supplied time.
     *
     * @param now current UTC instant
     * @param nextRetryAt next retry instant, or {@code null} for immediate work
     * @return {@code true} when the state is claimable and due
     */
    public boolean claimableAt(Instant now, Instant nextRetryAt) {
        Objects.requireNonNull(now, "now");
        if (this != DISCOVERED && !retryable() && this != STALE) {
            return false;
        }
        return nextRetryAt == null || !nextRetryAt.isAfter(now);
    }
}
