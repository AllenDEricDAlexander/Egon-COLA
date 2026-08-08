package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.List;

/**
 * Discovers transport-specific interface groups for a Gateway definition
 * report.
 *
 * 用于发现并汇总各传输协议暴露的网关接口分组。
 */
public interface GatewayDefinitionContributor {

    /**
     * Discovers the interface groups exposed by the contributor's transport.
     *
     * 发现当前贡献者对应传输协议暴露的接口分组。
     *
     * @return the discovered interface groups
     */
    List<DiscoveredInterfaceGroup> discover();

    /**
     * Associates a reported interface group with its business and entity
     * domains.
     *
     * 将接口分组报告与业务域和实体域信息关联起来。
     *
     * @param businessDomainCode        the business domain code，业务域编码
     * @param businessDomainName        the business domain name，业务域名称
     * @param businessDomainDescription the business domain description, or
     *                                  {@code null} when none is declared；业务域描述，未声明时为 {@code null}
     * @param entityDomainCode          the entity domain code，实体域编码
     * @param entityDomainName          the entity domain name，实体域名称
     * @param entityDomainDescription   the entity domain description, or
     *                                  {@code null} when none is declared；实体域描述，未声明时为 {@code null}
     * @param interfaceGroup            the discovered interface group report，已发现的接口分组报告
     */
    record DiscoveredInterfaceGroup(
            String businessDomainCode,
            String businessDomainName,
            String businessDomainDescription,
            String entityDomainCode,
            String entityDomainName,
            String entityDomainDescription,
            GatewayInterfaceDefinitionReport.InterfaceGroup interfaceGroup
    ) {
    }
}
