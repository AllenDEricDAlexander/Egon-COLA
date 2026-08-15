package top.egon.cola.component.ddc.model.management;

/**
 * DDC 业务域目录查询条件。 / Query filters for DDC business domains.
 *
 * @param keyword 关键字，可为空 / keyword, nullable
 * @param enabled 启用状态，可为空 / enabled state, nullable
 */
public record DdcManagementBizQuery(
        String keyword,
        Boolean enabled
) {
}
