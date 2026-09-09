package top.egon.cola.component.yuheng.admin.openapi.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;

/**
 * Structural MapStruct conversion between normalized OpenAPI DTOs and the
 * shared Gateway Report v2 contract.
 *
 * <p>中文：不在这里做 JSON round-trip 或图遍历；算法性映射保留在两个
 * Adapter，转换器只负责同构节点复制并实现 BaseConverter。</p>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface GatewayOpenApiDefinitionConverter extends BaseConverter<
        GatewayOpenApiDefinitionDTO,
        GatewayInterfaceDefinitionReport> {

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.ERROR)
    GatewayInterfaceDefinitionReport toTarget(
            GatewayOpenApiDefinitionDTO source);

    @Override
    @Mapping(
            target = "canonicalSha256",
            expression = "java(canonicalSha256(target))"
    )
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.ERROR)
    GatewayOpenApiDefinitionDTO toSource(
            GatewayInterfaceDefinitionReport target);

    GatewayInterfaceDefinitionReport.Application toTarget(
            GatewayOpenApiDefinitionDTO.Application source);

    GatewayInterfaceDefinitionReport.Build toTarget(
            GatewayOpenApiDefinitionDTO.Build source);

    GatewayInterfaceDefinitionReport.BusinessDomain toTarget(
            GatewayOpenApiDefinitionDTO.BusinessDomain source);

    GatewayInterfaceDefinitionReport.EntityDomain toTarget(
            GatewayOpenApiDefinitionDTO.EntityDomain source);

    GatewayInterfaceDefinitionReport.InterfaceGroup toTarget(
            GatewayOpenApiDefinitionDTO.InterfaceGroup source);

    GatewayInterfaceDefinitionReport.Operation toTarget(
            GatewayOpenApiDefinitionDTO.Operation source);

    GatewayInterfaceDefinitionReport.ProviderService toTarget(
            GatewayOpenApiDefinitionDTO.ProviderService source);

    GatewayOpenApiDefinitionDTO.Application toSource(
            GatewayInterfaceDefinitionReport.Application target);

    GatewayOpenApiDefinitionDTO.Build toSource(
            GatewayInterfaceDefinitionReport.Build target);

    GatewayOpenApiDefinitionDTO.BusinessDomain toSource(
            GatewayInterfaceDefinitionReport.BusinessDomain target);

    GatewayOpenApiDefinitionDTO.EntityDomain toSource(
            GatewayInterfaceDefinitionReport.EntityDomain target);

    GatewayOpenApiDefinitionDTO.InterfaceGroup toSource(
            GatewayInterfaceDefinitionReport.InterfaceGroup target);

    GatewayOpenApiDefinitionDTO.Operation toSource(
            GatewayInterfaceDefinitionReport.Operation target);

    GatewayOpenApiDefinitionDTO.ProviderService toSource(
            GatewayInterfaceDefinitionReport.ProviderService target);

    /**
     * Canonical source provenance is carried in Report Build.metadata during
     * the lossless normalized round trip.
     */
    default String canonicalSha256(
            GatewayInterfaceDefinitionReport target) {
        Object value = target == null || target.build() == null
                ? null
                : target.build().metadata().get("openapiCanonicalSha256");
        if (!(value instanceof String canonical)
                || !canonical.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Report metadata does not contain OpenAPI canonical SHA"
            );
        }
        return canonical;
    }
}
