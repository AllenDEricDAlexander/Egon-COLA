package top.egon.cola.platform.idp.admin.resource.support.rpc;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.platform.idp.admin.resource.service.impl.ResourceServerAdmissionServiceImpl;
import top.egon.cola.platform.idp.core.resource.AdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionResponse;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServerAdmissionRpcProviderTest {

    @Test
    void delegatesProtocolRequestToAdmissionService() {
        ResourceServerAdmissionServiceImpl admissions =
                mock(ResourceServerAdmissionServiceImpl.class);
        Instant expiresAt = Instant.parse("2026-08-12T08:05:00Z");
        when(admissions.issue(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(AdmissionRequest.class)
        )).thenReturn(new ResourceServerAdmissionServiceImpl.IssuedAdmissionTicket(
                "signed-ticket",
                expiresAt
        ));
        ResourceServerAdmissionRpcProvider provider =
                new ResourceServerAdmissionRpcProvider(admissions);
        IssueResourceServerAdmissionRequest request =
                IssueResourceServerAdmissionRequest.newBuilder()
                        .setClientAssertionType("assertion-type")
                        .setClientId("management-client")
                        .setClientAssertion("signed-assertion")
                        .setResourceServerId("resource-rbac3-local")
                        .setResource("https://api.example/local/permission/rbac3")
                        .setBiz("permission")
                        .setApp("rbac3")
                        .setEnv("local")
                        .setInstanceId("rbac3-local-1")
                        .build();

        IssueResourceServerAdmissionResponse response =
                provider.issueAdmission(request);

        ArgumentCaptor<AdmissionRequest> admission =
                ArgumentCaptor.forClass(AdmissionRequest.class);
        verify(admissions).issue(
                org.mockito.ArgumentMatchers.eq("assertion-type"),
                org.mockito.ArgumentMatchers.eq("management-client"),
                org.mockito.ArgumentMatchers.eq("signed-assertion"),
                admission.capture()
        );
        assertThat(admission.getValue()).isEqualTo(new AdmissionRequest(
                "resource-rbac3-local",
                URI.create("https://api.example/local/permission/rbac3"),
                "permission",
                "rbac3",
                "local",
                "rbac3-local-1"
        ));
        assertThat(response.getTicket()).isEqualTo("signed-ticket");
        assertThat(response.getExpiresAt()).isEqualTo(
                Timestamp.newBuilder()
                        .setSeconds(expiresAt.getEpochSecond())
                        .setNanos(expiresAt.getNano())
                        .build()
        );
    }
}
