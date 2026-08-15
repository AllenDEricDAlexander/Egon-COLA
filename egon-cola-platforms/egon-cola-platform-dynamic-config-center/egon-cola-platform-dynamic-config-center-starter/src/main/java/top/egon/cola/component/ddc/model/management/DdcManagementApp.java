package top.egon.cola.component.ddc.model.management;

/**
 * DDC 应用目录项。 / DDC application catalog entry.
 *
 * @param id              DDC 应用标识 / DDC application identifier
 * @param businessId      所属业务域标识 / owning business identifier
 * @param bizCode         所属业务编码 / owning business code
 * @param appCode         应用编码 / application code
 * @param appName         应用名称 / application name
 * @param enabled         应用是否启用 / whether the application is enabled
 * @param businessEnabled 父业务域是否启用 / whether the parent business is enabled
 */
public record DdcManagementApp(
        String id,
        String businessId,
        String bizCode,
        String appCode,
        String appName,
        boolean enabled,
        boolean businessEnabled
) {
}
