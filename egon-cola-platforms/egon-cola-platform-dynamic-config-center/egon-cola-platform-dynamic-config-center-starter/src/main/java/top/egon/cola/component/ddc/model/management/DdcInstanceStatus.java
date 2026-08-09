package top.egon.cola.component.ddc.model.management;

import java.time.Instant;
import java.util.Locale;

/**
 * DDC 租约实例的归一化在线状态。 / Normalized online status of a DDC lease instance.
 */
public enum DdcInstanceStatus {

    /**
     * 租约实例在线。 / The leased instance is online.
     */
    ONLINE,
    /**
     * 租约实例离线。 / The leased instance is offline.
     */
    OFFLINE,
    /**
     * 线格式状态为空或无法识别。 / The wire status is absent or unrecognized.
     */
    UNKNOWN;

    /**
     * 将兼容的线格式状态别名归一化为客户端状态。 / Normalizes compatible wire-status aliases to a client status.
     *
     * @param value 服务端返回的状态文本 / status text returned by the server
     * @return 归一化状态；空值或未知值返回 {@link #UNKNOWN} / normalized status; {@link #UNKNOWN} for blank or unknown values
     */
    public static DdcInstanceStatus fromWire(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ONLINE", "REGISTERED", "UP" -> ONLINE;
            case "OFFLINE", "EXPIRED", "DOWN" -> OFFLINE;
            default -> UNKNOWN;
        };
    }

    /**
     * 判断实例在指定时刻是否在线且租约尚未过期。 /
     * Determines whether the instance is online and its lease remains unexpired at the specified time.
     *
     * @param now           用于判断租约有效性的当前时刻 / current instant used to evaluate lease validity
     * @param leaseExpireAt 租约过期时间 / lease expiration time
     * @return 在线且租约过期时间晚于当前时刻时为 {@code true} / {@code true} when online and the lease expires after the current instant
     */
    public boolean isAvailable(Instant now, Instant leaseExpireAt) {
        return this == ONLINE && leaseExpireAt != null
                && leaseExpireAt.isAfter(now);
    }
}
