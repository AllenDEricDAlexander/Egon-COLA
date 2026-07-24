package top.egon.cola.component.rpc.context;

public record RpcProcessIdentity(
        String applicationName,
        String env,
        String namespace,
        String host,
        long pid,
        String instanceId
) {
}
