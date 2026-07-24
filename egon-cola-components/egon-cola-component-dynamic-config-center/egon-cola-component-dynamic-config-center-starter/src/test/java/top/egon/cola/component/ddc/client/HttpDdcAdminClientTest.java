package top.egon.cola.component.ddc.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.crypto.hmac.Hmacs;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpDdcAdminClientTest {

    @Test
    void signatureUsesAccessKeyTimestampPathAndSecret() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setAccessKey("ak");
        properties.getAdmin().setSecretKey("sk");
        HttpDdcAdminClient client = new HttpDdcAdminClient(properties);

        String signature = client.signature("/api/v1/ddc/openapi/publish/ack", 100L);

        assertThat(signature).isEqualTo(Hmacs.sha256Hex("ak|100|/api/v1/ddc/openapi/publish/ack", "sk"));
    }

    @Test
    void parsesLeaseRegisterHeartbeatAndOfflineEnvelopes() {
        ClientFixture fixture = fixture();
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "instanceId": "instance-1",
                            "leaseId": "lease-1",
                            "role": "CONFIG_CLIENT",
                            "leaseSeconds": 30,
                            "heartbeatIntervalSeconds": 10,
                            "registeredAt": "2026-07-24T12:00:00Z",
                            "leaseExpireAt": "2026-07-24T12:00:30Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/heartbeat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "status": "RENEWED",
                            "leaseExpireAt": "2026-07-24T12:01:00Z"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/offline"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 0,
                          "status": "SUCCESS",
                          "message": "success",
                          "data": {
                            "status": "DELETED",
                            "leaseExpireAt": null
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DdcLeaseSession session = fixture.client().register(new DdcInstanceRegisterRequest());
        DdcLeaseOperationResult heartbeat = fixture.client().heartbeat(new DdcHeartbeatRequest());
        DdcLeaseOperationResult offline = fixture.client().offline(new DdcHeartbeatRequest());

        assertThat(session.instanceId()).isEqualTo("instance-1");
        assertThat(session.leaseId()).isEqualTo("lease-1");
        assertThat(session.role()).isEqualTo(DdcLeaseRole.CONFIG_CLIENT);
        assertThat(heartbeat.status()).isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(heartbeat.renewed()).isTrue();
        assertThat(offline.status()).isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(offline.deleted()).isTrue();
        fixture.server().verify();
    }

    @Test
    void failureEnvelopeBecomesTypedDdcException() {
        ClientFixture fixture = fixture();
        fixture.server().expect(requestTo("http://ddc.test/api/v1/ddc/openapi/instances/register"))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "code": 56002,
                          "status": "DDC_LEASE_MISMATCH",
                          "message": "lease mismatch",
                          "data": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client().register(new DdcInstanceRegisterRequest()))
                .isInstanceOfSatisfying(DdcException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(56002);
                    assertThat(exception.getStatus()).isEqualTo("DDC_LEASE_MISMATCH");
                    assertThat(exception.getMessage()).isEqualTo("lease mismatch");
                });
        fixture.server().verify();
    }

    private ClientFixture fixture() {
        DdcProperties properties = new DdcProperties();
        properties.getAdmin().setEndpoint("http://ddc.test");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getAdmin().getEndpoint());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new ClientFixture(new HttpDdcAdminClient(properties, builder), server);
    }

    private record ClientFixture(
            HttpDdcAdminClient client,
            MockRestServiceServer server
    ) {
    }
}
