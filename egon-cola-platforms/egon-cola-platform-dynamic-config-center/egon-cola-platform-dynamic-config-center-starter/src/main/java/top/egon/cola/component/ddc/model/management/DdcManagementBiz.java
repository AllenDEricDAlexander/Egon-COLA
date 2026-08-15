package top.egon.cola.component.ddc.model.management;

/**
 * DDC 业务域目录项。 / DDC business-domain catalog entry.
 *
 * @param id        DDC 业务域标识 / DDC business identifier
 * @param bizCode   业务编码 / business code
 * @param bizName   业务名称 / business name
 * @param enabled   业务域是否启用 / whether the business is enabled
 */
public record DdcManagementBiz(
        String id,
        String bizCode,
        String bizName,
        boolean enabled
) {
}
