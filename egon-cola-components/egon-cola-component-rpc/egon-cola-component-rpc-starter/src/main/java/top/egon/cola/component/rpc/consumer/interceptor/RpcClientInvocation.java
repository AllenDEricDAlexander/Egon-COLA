package top.egon.cola.component.rpc.consumer.interceptor;

import com.google.protobuf.Message;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericInvocation;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;

import java.util.Arrays;
import java.util.Objects;

/** Immutable request context shared by typed and restricted generic interceptors. */
public final class RpcClientInvocation {

    private final RpcContractDescriptor contract;
    private final RpcMethodDescriptor method;
    private final Message request;
    private final RpcProcessIdentity processIdentity;
    private final String serviceName;
    private final String group;
    private final String version;
    private final String fullMethodName;
    private final byte[] rawRequestPayload;
    private final String invocationId;

    /** Creates the legacy typed context retained for existing factories. */
    public RpcClientInvocation(
            RpcContractDescriptor contract,
            RpcMethodDescriptor method,
            Message request,
            RpcProcessIdentity processIdentity) {
        this(
                Objects.requireNonNull(contract, "contract"),
                Objects.requireNonNull(method, "method"),
                Objects.requireNonNull(request, "request"),
                Objects.requireNonNull(processIdentity, "processIdentity"),
                contract.serviceName(),
                contract.group(),
                contract.version(),
                method.fullMethodName(),
                null,
                null
        );
    }

    private RpcClientInvocation(
            RpcContractDescriptor contract,
            RpcMethodDescriptor method,
            Message request,
            RpcProcessIdentity processIdentity,
            String serviceName,
            String group,
            String version,
            String fullMethodName,
            byte[] rawRequestPayload,
            String invocationId) {
        this.contract = contract;
        this.method = method;
        this.request = request;
        this.processIdentity = Objects.requireNonNull(
                processIdentity,
                "processIdentity"
        );
        this.serviceName = required(serviceName, "serviceName");
        this.group = required(group, "group");
        this.version = required(version, "version");
        this.fullMethodName = required(fullMethodName, "fullMethodName");
        if ((request == null) == (rawRequestPayload == null)) {
            throw new IllegalArgumentException(
                    "RPC invocation must contain exactly one typed or raw request"
            );
        }
        this.rawRequestPayload = rawRequestPayload == null
                ? null : Arrays.copyOf(rawRequestPayload, rawRequestPayload.length);
        this.invocationId = invocationId == null || invocationId.isBlank()
                ? null : invocationId.trim();
    }

    public static RpcClientInvocation generic(
            RpcGenericInvocation invocation,
            RpcProcessIdentity processIdentity,
            String invocationId) {
        Objects.requireNonNull(invocation, "invocation");
        return new RpcClientInvocation(
                null,
                null,
                null,
                processIdentity,
                invocation.serviceName(),
                invocation.group(),
                invocation.version(),
                invocation.fullMethodName(),
                invocation.requestPayload(),
                invocationId
        );
    }

    public static RpcClientInvocation typed(
            RpcContractDescriptor contract,
            RpcMethodDescriptor method,
            Message request,
            RpcProcessIdentity processIdentity,
            String group,
            String version,
            String invocationId) {
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(request, "request");
        return new RpcClientInvocation(
                contract,
                method,
                request,
                processIdentity,
                contract.serviceName(),
                group,
                version,
                method.fullMethodName(),
                null,
                invocationId
        );
    }

    public RpcContractDescriptor contract() {
        return contract;
    }

    public RpcMethodDescriptor method() {
        return method;
    }

    public Message request() {
        return request;
    }

    public RpcProcessIdentity processIdentity() {
        return processIdentity;
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

    public boolean generic() {
        return rawRequestPayload != null;
    }

    public byte[] rawRequestPayload() {
        return rawRequestPayload == null
                ? null : Arrays.copyOf(rawRequestPayload, rawRequestPayload.length);
    }

    public String invocationId() {
        return invocationId;
    }

    private static String required(String value, String name) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
