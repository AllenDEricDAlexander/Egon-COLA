package top.egon.cola.component.tianshu.model.management;

/**
 * Tianshu 业务域单项查询定位条件。 / Locator for a single Tianshu business lookup.
 *
 * @param id      Tianshu 业务域标识，可为空 / Tianshu business identifier, nullable
 * @param bizCode 业务编码，可为空 / business code, nullable
 */
public record DdcManagementBizLookup(
        String id,
        String bizCode
) {
}
