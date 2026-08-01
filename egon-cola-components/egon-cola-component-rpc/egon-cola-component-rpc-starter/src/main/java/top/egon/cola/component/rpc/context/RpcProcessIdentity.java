package top.egon.cola.component.rpc.context;

public record RpcProcessIdentity(
        String applicationName,
        String env,
        String host,
        long pid,
        String instanceId
) {

    public RpcProcessIdentity(
            String applicationName,
            String env,
            String namespace,
            String host,
            long pid,
            String instanceId) {
        this(applicationName, env, host, pid, instanceId);
    }

    /**
     * @deprecated namespace is a visibility binding and is not part of the
     * physical RPC process identity.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
    }
}
