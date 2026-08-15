package top.egon.cola.component.ddc.model.management;

/**
 * DDC 业务域单项查询定位条件。 / Locator for a single DDC business lookup.
 *
 * @param id      DDC 业务域标识，可为空 / DDC business identifier, nullable
 * @param bizCode 业务编码，可为空 / business code, nullable
 */
public record DdcManagementBizLookup(
        String id,
        String bizCode
) {
}
