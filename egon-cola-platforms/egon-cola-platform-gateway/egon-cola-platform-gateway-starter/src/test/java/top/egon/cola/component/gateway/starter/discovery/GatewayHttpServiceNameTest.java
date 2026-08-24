package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.discovery.http.GatewayHttpOperationMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayHttpServiceNameTest {

    @Test
    void reportsTheDeclaredHttpServiceNameAsTheProviderServiceName()
            throws Exception {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setBizCode("permission");
        properties.setApplicationCode("idp");
        properties.setEnv("local");
        properties.setNamespace("default");
        properties.setArtifactVersion("local");
        GatewayHttpOperationMapper mapper = new GatewayHttpOperationMapper(
                properties,
                new ObjectMapper()
        );
        var method = DeclaredService.class.getDeclaredMethod("status");

        GatewayInterfaceDefinitionReport.Operation operation = mapper.group(
                DeclaredService.class,
                List.of(new GatewayHttpOperationMapper.Mapping(
                        new HandlerMethod(new DeclaredService(), method),
                        Set.of("/status"),
                        Set.of("GET"),
                        Set.of(),
                        Set.of()
                ))
        ).interfaceGroup().operations().getFirst();

        assertThat(operation.providerService().serviceName())
                .isEqualTo("idp-admin");
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "platform",
            businessDomainName = "平台治理域",
            entityDomainCode = "identity",
            entityDomainName = "统一身份",
            code = "identity",
            name = "统一身份")
    @EgonHttpService(serviceName = "idp-admin")
    private static final class DeclaredService {

        @GetMapping
        @GatewayOperation(externalAccessible = true)
        String status() {
            return "ok";
        }
    }
}
