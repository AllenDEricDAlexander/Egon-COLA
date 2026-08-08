package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.List;

/**
 * Discovers transport-specific interface groups for a Gateway definition
 * report.
 */
public interface GatewayDefinitionContributor {

    /**
     * Discovers the interface groups exposed by the contributor's transport.
     *
     * @return the discovered interface groups
     */
    List<DiscoveredInterfaceGroup> discover();

    /**
     * Associates a reported interface group with its business and entity
     * domains.
     *
     * @param businessDomainCode        the business domain code
     * @param businessDomainName        the business domain name
     * @param businessDomainDescription the business domain description, or
     *                                  {@code null} when none is declared
     * @param entityDomainCode          the entity domain code
     * @param entityDomainName          the entity domain name
     * @param entityDomainDescription   the entity domain description, or
     *                                  {@code null} when none is declared
     * @param interfaceGroup            the discovered interface group report
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
