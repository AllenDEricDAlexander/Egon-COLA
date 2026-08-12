package top.egon.cola.platform.idp.starter.admission;

import com.google.protobuf.Timestamp;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.platform.idp.rpc.contract.ResourceServerAdmissionRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionResponse;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcResourceServerAdmissionClientTest {

    private static final Instant NOW = Instant.parse("2026-08-12T08:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plusSeconds(300);
    private static final String ISSUER = "https://idp.example";

    @Test
    void mapsExactAdmissionIdentityAndParsesTrustedResponse() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).generate();
        DdcAdmissionRequest request = request();
        String ticketJwt = admissionJwt(key, request, "management-key-1");
        AtomicReference<IssueResourceServerAdmissionRequest> captured =
                new AtomicReference<>();
        ResourceServerAdmissionRpc rpc = rpcRequest -> {
            captured.set(rpcRequest);
            return IssueResourceServerAdmissionResponse.newBuilder()
                    .setTicket(ticketJwt)
                    .setExpiresAt(timestamp(EXPIRES_AT))
                    .build();
        };
        RpcResourceServerAdmissionClient client =
                new RpcResourceServerAdmissionClient(
                        rpc,
                        ISSUER,
                        assertions(key)
                );

        DdcAdmissionTicket ticket = client.request(request);

        IssueResourceServerAdmissionRequest rpcRequest = captured.get();
        assertThat(rpcRequest.getClientAssertionType()).isEqualTo(
                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
        );
        assertThat(rpcRequest.getClientId()).isEqualTo("management-client");
        assertThat(SignedJWT.parse(rpcRequest.getClientAssertion())
                .getJWTClaimsSet().getAudience())
                .containsExactly(ResourceServerAdmissionRpc.AUDIENCE.toString());
        assertThat(rpcRequest.getResourceServerId())
                .isEqualTo(request.resourceServerId());
        assertThat(rpcRequest.getResource()).isEqualTo(
                request.resourceUri().toString()
        );
        assertThat(rpcRequest.getBiz()).isEqualTo(request.bizCode());
        assertThat(rpcRequest.getApp()).isEqualTo(request.appCode());
        assertThat(rpcRequest.getEnv()).isEqualTo(request.environment());
        assertThat(rpcRequest.getInstanceId()).isEqualTo(request.instanceId());
        assertThat(ticket.matches(request)).isTrue();
        assertThat(ticket.credentialId()).isEqualTo("management-key-1");
        assertThat(ticket.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void failsClosedWhenRpcTransportFails() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).generate();
        ResourceServerAdmissionRpc rpc = request -> {
            throw new IllegalStateException("transport unavailable");
        };
        RpcResourceServerAdmissionClient client =
                new RpcResourceServerAdmissionClient(
                        rpc,
                        ISSUER,
                        assertions(key)
                );

        assertThatThrownBy(() -> client.request(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IDP_RESOURCE_ADMISSION_UNAVAILABLE")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private static PrivateKeyJwtAssertionFactory assertions(RSAKey key)
            throws Exception {
        return new PrivateKeyJwtAssertionFactory(
                "management-client",
                "management-key-1",
                ResourceServerAdmissionRpc.AUDIENCE,
                key.toRSAPrivateKey(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SecureRandom()
        );
    }

    private static DdcAdmissionRequest request() {
        return new DdcAdmissionRequest(
                "resource-rbac3-local",
                URI.create("https://api.example/local/permission/rbac3"),
                "permission",
                "rbac3",
                "local",
                "rbac3-local-1"
        );
    }

    private static String admissionJwt(
            RSAKey key,
            DdcAdmissionRequest request,
            String credentialId
    ) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(new JOSEObjectType("rs-admission+jwt"))
                        .build(),
                new JWTClaimsSet.Builder()
                        .issuer(ISSUER)
                        .subject(request.resourceServerId())
                        .audience(List.of("ddc-registry"))
                        .claim("token_use", "resource_server_admission")
                        .claim("resource", request.resourceUri().toString())
                        .claim("resource_version", 7L)
                        .claim("biz", request.bizCode())
                        .claim("app", request.appCode())
                        .claim("env", request.environment())
                        .claim("instance_id", request.instanceId())
                        .claim("credential_id", credentialId)
                        .issueTime(Date.from(NOW))
                        .expirationTime(Date.from(EXPIRES_AT))
                        .build()
        );
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
