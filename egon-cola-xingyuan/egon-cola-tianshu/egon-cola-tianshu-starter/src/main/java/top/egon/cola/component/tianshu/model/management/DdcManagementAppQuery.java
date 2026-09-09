package top.egon.cola.component.tianshu.model.management;

/**
 * Tianshu 应用目录查询条件。 / Query filters for Tianshu applications.
 *
 * @param businessId 所属业务域标识，可为空 / business identifier, nullable
 * @param bizCode    所属业务编码，可为空 / business code, nullable
 * @param keyword    关键字，可为空 / keyword, nullable
 * @param enabled    启用状态，可为空 / enabled state, nullable
 */
public record DdcManagementAppQuery(
        String businessId,
        String bizCode,
        String keyword,
        Boolean enabled
) {
}
