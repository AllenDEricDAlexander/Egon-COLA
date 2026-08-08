package top.egon.cola.component.ddc.model.registry;

/**
 * 注册中心通过消息主题发布的服务变更事件。
 * / Service change event published through the registry topic.
 *
 * @param serviceKey 已变更的服务键 / changed service key
 * @param serviceRevision 服务实例集合修订号 / service instance set revision
 * @param catalogRevision 服务目录修订号 / service catalog revision
 */
public record DdcRegistryEvent(
        DdcServiceKey serviceKey,
        long serviceRevision,
        long catalogRevision
) {
}
