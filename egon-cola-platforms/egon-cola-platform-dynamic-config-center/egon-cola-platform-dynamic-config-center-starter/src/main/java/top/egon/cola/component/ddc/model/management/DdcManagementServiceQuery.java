package top.egon.cola.component.ddc.model.management;

import org.springframework.lang.Nullable;

/**
 * DDC 服务目录与实例快照的查询条件。 / Query filters for DDC service catalogs and instance snapshots.
 *
 * @param bizCode       业务编码，可为空以不按该项过滤 / business code, nullable to omit this filter
 * @param namespaceCode 用于授权与可见性过滤的命名空间编码 / namespace code used for authorization and visibility filtering
 * @param env           环境编码，可为空以不按该项过滤 / environment code, nullable to omit this filter
 * @param appCode       应用编码，可为空以不按该项过滤 / application code, nullable to omit this filter
 * @param serviceKind   服务类型，可为空以不按该项过滤 / service kind, nullable to omit this filter
 * @param protocol      服务协议，可为空以不按该项过滤 / service protocol, nullable to omit this filter
 * @param serviceName   服务名称，可为空以不按该项过滤 / service name, nullable to omit this filter
 * @param group         服务分组，可为空以不按该项过滤 / service group, nullable to omit this filter
 * @param version       服务版本，可为空以不按该项过滤 / service version, nullable to omit this filter
 */
public record DdcManagementServiceQuery(
        @Nullable String bizCode,
        String namespaceCode,
        @Nullable String env,
        @Nullable String appCode,
        @Nullable String serviceKind,
        @Nullable String protocol,
        @Nullable String serviceName,
        @Nullable String group,
        @Nullable String version
) {
}
