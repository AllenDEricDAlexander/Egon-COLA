package top.egon.cola.component.gateway.starter.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.ddc.client.http.DdcRequestSigner;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GatewayReportHttpClientTest {

    @Test
    void signsTheExactImmutablePayload() {
        GatewayReportingProperties properties = properties();
        GatewayDefinitionReportFactory.BuiltReport report =
                new GatewayDefinitionReportFactory(
                        properties,
                        Clock.fixed(
                                Instant.parse("2026-07-25T00:00:00Z"),
                                ZoneOffset.UTC
                        )
                ).build(List.of());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://admin.local");
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(
                        "http://admin.local"
                                + GatewayReportHttpClient.REPORT_PATH
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        DdcRequestSigner.ACCESS_KEY_HEADER,
                        "access-key"
                ))
                .andExpect(header(
                        "X-Gateway-Report-Id",
                        report.report().reportId()
                ))
                .andRespond(withSuccess("""
                        {
                          "reportId":"%s",
                          "definitionSetId":"%s",
                          "status":"ACCEPTED",
                          "applicationId":"app-1",
                          "counts":{
                            "businessDomains":0,
                            "entityDomains":0,
                            "interfaceGroups":0,
                            "operations":0,
                            "created":0,
                            "updated":0,
                            "missingFromThisSet":0
                          },
                          "operationRefs":[],
                          "warnings":[],
                          "receivedAt":"2026-07-25T00:00:01Z"
                        }
                        """.formatted(
                        report.report().reportId(),
                        report.report().definitionSetId()
                ), MediaType.APPLICATION_JSON));
        GatewayReportHttpClient client = new GatewayReportHttpClient(
                properties,
                builder.build(),
                Clock.fixed(
                        Instant.ofEpochMilli(1_753_401_600_000L),
                        ZoneOffset.UTC
                )
        );

        GatewayInterfaceDefinitionReportResult result =
                client.submit(report);

        assertThat(result.status()).isEqualTo(
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED
        );
        server.verify();
    }

    private GatewayReportingProperties properties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://admin.local");
        properties.setApplicationCode("inventory");
        properties.setBizCode("test-biz");
        properties.setApplicationName("Inventory");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");
        return properties;
    }
}
