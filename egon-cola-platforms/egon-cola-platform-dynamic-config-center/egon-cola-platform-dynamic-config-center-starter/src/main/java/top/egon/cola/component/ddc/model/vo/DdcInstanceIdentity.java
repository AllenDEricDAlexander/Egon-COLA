package top.egon.cola.component.ddc.model.vo;

public record DdcInstanceIdentity(
        String instanceId,
        String bizCode,
        String appCode,
        String env,
        String host,
        Integer port,
        String pid,
        String sdkVersion
) {

    /**
     * @deprecated namespace is a visibility view and is not part of physical
     * instance identity.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return null;
    }
}
