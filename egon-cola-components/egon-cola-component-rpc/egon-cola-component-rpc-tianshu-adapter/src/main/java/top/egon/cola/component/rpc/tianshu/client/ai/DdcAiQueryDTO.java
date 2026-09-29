package top.egon.cola.component.rpc.tianshu.client.ai;

/** Immutable, fully scoped query assembled from local Tianshu properties and a public selector. */
public record DdcAiQueryDTO(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        String resourceKey,
        Long versionNo,
        Long resourceId) {
}
