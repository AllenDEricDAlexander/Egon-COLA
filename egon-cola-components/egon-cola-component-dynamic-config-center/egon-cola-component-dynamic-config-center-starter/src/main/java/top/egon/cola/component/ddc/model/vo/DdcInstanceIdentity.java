package top.egon.cola.component.ddc.model.vo;

public record DdcInstanceIdentity(
        String instanceId,
        String appCode,
        String env,
        String namespace,
        String host,
        Integer port,
        String pid,
        String sdkVersion
) {
}
