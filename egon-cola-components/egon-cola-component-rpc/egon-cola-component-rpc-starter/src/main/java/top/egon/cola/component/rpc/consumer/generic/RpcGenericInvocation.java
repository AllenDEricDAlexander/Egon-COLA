package top.egon.cola.component.rpc.consumer.generic;

import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Immutable, credential-free command for one raw unary invocation. */
public final class RpcGenericInvocation {

    private static final Pattern SAFE_SEGMENT = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}"
    );

    private static final Pattern METHOD_SEGMENT = Pattern.compile(
            "[A-Za-z0-9_][A-Za-z0-9_.]{0,127}"
    );

    private final RpcReferenceMode mode;
    private final String bizCode;
    private final String appCode;
    private final String env;
    private final String serviceName;
    private final String group;
    private final String version;
    private final String fullMethodName;
    private final byte[] requestPayload;
    private final long timeoutMs;
    private final int retries;
    private final LoadBalance loadBalance;
    private final FailStrategy failStrategy;
    private final String affinityKey;
    private final Function<byte[], byte[]> fallback;

    public RpcGenericInvocation(
            RpcReferenceMode mode,
            String bizCode,
            String appCode,
            String env,
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            byte[] requestPayload,
            long timeoutMs,
            int retries,
            LoadBalance loadBalance,
            FailStrategy failStrategy,
            String affinityKey) {
        this(
                mode,
                bizCode,
                appCode,
                env,
                serviceName,
                group,
                version,
                fullMethodName,
                requestPayload,
                timeoutMs,
                retries,
                loadBalance,
                failStrategy,
                affinityKey,
                null
        );
    }

    public RpcGenericInvocation(
            RpcReferenceMode mode,
            String bizCode,
            String appCode,
            String env,
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            byte[] requestPayload,
            long timeoutMs,
            int retries,
            LoadBalance loadBalance,
            FailStrategy failStrategy,
            String affinityKey,
            Function<byte[], byte[]> fallback) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.bizCode = optionalSegment(bizCode, "bizCode");
        this.appCode = optionalSegment(appCode, "appCode");
        this.env = optionalSegment(env, "env");
        this.serviceName = requiredSegment(serviceName, "serviceName");
        this.group = requiredSegment(group, "group");
        this.version = requiredSegment(version, "version");
        this.fullMethodName = canonicalFullMethodName(
                this.serviceName,
                fullMethodName
        );
        if (requestPayload == null) {
            throw invalid("generic request payload is required");
        }
        this.requestPayload = Arrays.copyOf(
                requestPayload,
                requestPayload.length
        );
        if (timeoutMs <= 0) {
            throw invalid("generic timeout must be positive");
        }
        this.timeoutMs = timeoutMs;
        if (retries < 0 || retries > 10) {
            throw invalid("generic retries must be between 0 and 10");
        }
        this.retries = retries;
        if (loadBalance == null || loadBalance == LoadBalance.INHERIT) {
            throw invalid("generic load-balance algorithm must be resolved");
        }
        this.loadBalance = loadBalance;
        if (failStrategy == null || failStrategy == FailStrategy.INHERIT) {
            throw invalid("generic fail strategy must be resolved");
        }
        this.failStrategy = failStrategy;
        String normalizedAffinity = affinityKey == null
                ? null : affinityKey.trim();
        if (loadBalance == LoadBalance.CONSISTENT_HASH) {
            if (normalizedAffinity == null || normalizedAffinity.isBlank()) {
                throw invalid("CONSISTENT_HASH requires an affinity key");
            }
            int affinityBytes = normalizedAffinity.getBytes(
                    StandardCharsets.UTF_8).length;
            if (affinityBytes > 512) {
                throw invalid("generic affinity key must be at most 512 UTF-8 bytes");
            }
        } else if (normalizedAffinity != null && !normalizedAffinity.isBlank()) {
            throw invalid("affinity key is only valid for CONSISTENT_HASH");
        }
        this.affinityKey = normalizedAffinity == null || normalizedAffinity.isBlank()
                ? null : normalizedAffinity;
        if (failStrategy == FailStrategy.LOCAL_FALLBACK && fallback == null) {
            throw invalid("LOCAL_FALLBACK requires a generic fallback");
        }
        if (failStrategy != FailStrategy.LOCAL_FALLBACK && fallback != null) {
            throw invalid("generic fallback is only valid with LOCAL_FALLBACK");
        }
        if (mode == RpcReferenceMode.DIRECT) {
            requiredSegment(this.bizCode, "bizCode");
            requiredSegment(this.appCode, "appCode");
            requiredSegment(this.env, "env");
        } else {
            if (this.bizCode != null || this.appCode != null || this.env != null) {
                throw invalid("Gateway generic invocation cannot carry provider query");
            }
        }
        this.fallback = fallback;
    }

    public static RpcGenericInvocation gateway(
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            byte[] requestPayload,
            long timeoutMs,
            int retries,
            LoadBalance loadBalance,
            FailStrategy failStrategy,
            String affinityKey) {
        return new RpcGenericInvocation(
                RpcReferenceMode.GATEWAY,
                null,
                null,
                null,
                serviceName,
                group,
                version,
                fullMethodName,
                requestPayload,
                timeoutMs,
                retries,
                loadBalance,
                failStrategy,
                affinityKey
        );
    }

    public static RpcGenericInvocation direct(
            String bizCode,
            String appCode,
            String env,
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            byte[] requestPayload,
            long timeoutMs,
            int retries,
            LoadBalance loadBalance,
            FailStrategy failStrategy,
            String affinityKey) {
        return new RpcGenericInvocation(
                RpcReferenceMode.DIRECT,
                bizCode,
                appCode,
                env,
                serviceName,
                group,
                version,
                fullMethodName,
                requestPayload,
                timeoutMs,
                retries,
                loadBalance,
                failStrategy,
                affinityKey
        );
    }

    public RpcReferenceMode mode() {
        return mode;
    }

    public String bizCode() {
        return bizCode;
    }

    public String appCode() {
        return appCode;
    }

    public String env() {
        return env;
    }

    public String serviceName() {
        return serviceName;
    }

    public String group() {
        return group;
    }

    public String version() {
        return version;
    }

    public String fullMethodName() {
        return fullMethodName;
    }

    public byte[] requestPayload() {
        return Arrays.copyOf(requestPayload, requestPayload.length);
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public int retries() {
        return retries;
    }

    public LoadBalance loadBalance() {
        return loadBalance;
    }

    public FailStrategy failStrategy() {
        return failStrategy;
    }

    public String affinityKey() {
        return affinityKey;
    }

    public Function<byte[], byte[]> fallback() {
        return fallback;
    }

    private static String canonicalFullMethodName(
            String serviceName,
            String fullMethodName) {
        if (fullMethodName == null) {
            throw invalid("generic fullMethodName is required");
        }
        String normalized = fullMethodName.trim();
        int slash = normalized.indexOf('/');
        if (slash <= 0 || slash != normalized.lastIndexOf('/')
                || slash == normalized.length() - 1) {
            throw invalid(
                    "generic fullMethodName must be canonical Service/Method");
        }
        String methodService = normalized.substring(0, slash);
        String method = normalized.substring(slash + 1);
        if (!serviceName.equals(methodService)
                || !SAFE_SEGMENT.matcher(methodService).matches()
                || !METHOD_SEGMENT.matcher(method).matches()) {
            throw invalid("generic fullMethodName does not match service identity");
        }
        return normalized;
    }

    private static String requiredSegment(String value, String name) {
        String normalized = optionalSegment(value, name);
        if (normalized == null) {
            throw invalid(name + " is required");
        }
        return normalized;
    }

    private static String optionalSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!SAFE_SEGMENT.matcher(normalized).matches()) {
            throw invalid(name + " is invalid");
        }
        return normalized;
    }

    private static EgonRpcException invalid(String message) {
        return new EgonRpcException(EgonRpcErrorCode.RPC_INVALID_REQUEST, message);
    }
}
