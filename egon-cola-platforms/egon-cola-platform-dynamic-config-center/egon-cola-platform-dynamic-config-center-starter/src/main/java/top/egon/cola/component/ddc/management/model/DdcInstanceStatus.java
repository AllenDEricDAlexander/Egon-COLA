package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Locale;

public enum DdcInstanceStatus {

    ONLINE,
    OFFLINE,
    UNKNOWN;

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

    public boolean isAvailable(Instant now, Instant leaseExpireAt) {
        return this == ONLINE && leaseExpireAt != null
                && leaseExpireAt.isAfter(now);
    }
}
