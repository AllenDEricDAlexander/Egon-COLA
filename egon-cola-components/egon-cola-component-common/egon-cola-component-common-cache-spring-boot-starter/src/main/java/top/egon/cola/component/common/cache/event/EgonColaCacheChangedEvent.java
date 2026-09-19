package top.egon.cola.component.common.cache.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 全模块唯一缓存变更事件信封（REQ-004 冻结字段序）。紧凑构造器是键与区域名规范化
 * 的唯一入口；守卫异常消息为 §13.2 冻结的 SCREAMING_SNAKE 错误码字面串。
 *
 * @param schemaVersion  事件协议版本，必须等于 {@link #SCHEMA_VERSION}
 * @param eventId        追踪诊断用非空白标识
 * @param originNodeId   发布节点 id，监听端据此跳过自回声
 * @param occurredAt     发生时间（UTC）
 * @param cacheName      缓存区域名，{@code [A-Za-z0-9_.-]{1,100}}
 * @param operation      操作类型
 * @param keys           同租户键集合：EVICT/PUT 为精确键 {@code tenantId:id}，
 *                       PREFIX_EVICT 仅允许 glob {@code tenantId:*}
 */
public record EgonColaCacheChangedEvent(int schemaVersion, String eventId, String originNodeId,
                                        Instant occurredAt, String cacheName,
                                        EgonColaCacheChangedOperation operation, List<String> keys) {

    public static final int SCHEMA_VERSION = 1;

    public EgonColaCacheChangedEvent {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("CACHE_EVENT_UNSUPPORTED_SCHEMA: " + schemaVersion);
        }
        requireText(eventId, "eventId");
        requireText(originNodeId, "originNodeId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(operation, "operation");
        KeyGuard.requireValidName(cacheName);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        for (String key : keys) {
            if (operation == EgonColaCacheChangedOperation.PREFIX_EVICT) {
                KeyGuard.requireGlob(key);
            } else {
                KeyGuard.requireExactKey(key);
            }
        }
        KeyGuard.requireSameTenant(keys);
        keys = List.copyOf(keys);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    /**
     * 键规则单点（PC-001 嵌套裁决）：信封构造、端口登记守卫与监听端二次校验共用。
     */
    public static final class KeyGuard {

        private static final Pattern EXACT = Pattern.compile("^\\d+:\\d+$");
        private static final Pattern GLOB = Pattern.compile("^\\d+:\\*$");
        private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");

        private KeyGuard() {
        }

        public static void requireValidName(String cacheName) {
            if (cacheName == null || !NAME.matcher(cacheName).matches()) {
                throw new IllegalArgumentException("CACHE_NAME_INVALID: " + cacheName);
            }
        }

        public static void requireExactKey(String key) {
            if (key != null && key.indexOf('*') >= 0) {
                throw new IllegalArgumentException("CACHE_GLOB_PATTERN_FORBIDDEN: " + key);
            }
            if (key == null || !EXACT.matcher(key).matches()) {
                throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + key);
            }
        }

        public static void requireGlob(String key) {
            if (key == null || !GLOB.matcher(key).matches()) {
                throw new IllegalArgumentException("CACHE_GLOB_PATTERN_FORBIDDEN: " + key);
            }
        }

        public static long tenantOf(String key) {
            requireExactKey(key);
            return parseTenant(key);
        }

        private static long parseTenant(String key) {
            return Long.parseLong(key.substring(0, key.indexOf(':')));
        }

        public static void requireTenant(String key, long currentTenantId) {
            if (tenantOf(key) != currentTenantId) {
                throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + key);
            }
        }

        public static void requireSameTenant(Collection<String> keys) {
            long tenant = parseTenant(keys.iterator().next());
            for (String key : keys) {
                if (parseTenant(key) != tenant) {
                    throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + key);
                }
            }
        }
    }
}
