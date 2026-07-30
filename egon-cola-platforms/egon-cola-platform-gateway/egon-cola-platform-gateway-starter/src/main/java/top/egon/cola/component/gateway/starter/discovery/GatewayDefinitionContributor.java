package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.util.List;

public interface GatewayDefinitionContributor {

    List<DiscoveredInterfaceGroup> discover();

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
