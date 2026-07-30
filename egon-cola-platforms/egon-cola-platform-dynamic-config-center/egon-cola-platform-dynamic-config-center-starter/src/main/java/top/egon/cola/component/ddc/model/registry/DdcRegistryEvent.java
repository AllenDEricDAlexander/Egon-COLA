package top.egon.cola.component.ddc.model.registry;

public record DdcRegistryEvent(
        DdcServiceKey serviceKey,
        long serviceRevision,
        long catalogRevision
) {
}
