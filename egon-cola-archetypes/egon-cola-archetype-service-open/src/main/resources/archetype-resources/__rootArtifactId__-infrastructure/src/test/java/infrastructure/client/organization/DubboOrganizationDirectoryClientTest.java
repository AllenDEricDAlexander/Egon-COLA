#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.organization.OrganizationSchoolClass;
import ${package}.domain.client.organization.OrganizationUser;
import ${package}.facade.organization.v1.GetSchoolClassRequest;
import ${package}.facade.organization.v1.GetUserRequest;
import ${package}.facade.organization.v1.SchoolClass;
import ${package}.facade.organization.v1.SchoolClassService;
import ${package}.facade.organization.v1.User;
import ${package}.facade.organization.v1.UserService;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DubboOrganizationDirectoryClientTest {

    private UserService userService;
    private SchoolClassService schoolClassService;
    private DubboOrganizationDirectoryClient client;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        schoolClassService = mock(SchoolClassService.class);
        client = new DubboOrganizationDirectoryClient(userService, schoolClassService);
    }

    @Test
    void mapsOrganizationUserToConsumerProjection() {
        when(userService.getUser(any(GetUserRequest.class))).thenReturn(User.newBuilder()
                .setId(1001L).setName("Mario").setEmail("m@example.com").setStatus("ACTIVE")
                .addRoleCodes("STUDENT").build());

        assertThat(client.getUser(1001L))
                .isEqualTo(new OrganizationUser(1001L, "Mario", "ACTIVE"));
    }

    @Test
    void mapsOrganizationSchoolClassToConsumerProjection() {
        when(schoolClassService.getSchoolClass(any(GetSchoolClassRequest.class))).thenReturn(SchoolClass.newBuilder()
                .setId(2001L).setName("Class One").setGradeCode("G1")
                .setGradeName("Grade One").setStatus("ACTIVE").addUserIds(1001L).build());

        assertThat(client.getSchoolClass(3001L, 2001L)).isEqualTo(new OrganizationSchoolClass(
                2001L, "Class One", "G1", "ACTIVE", List.of(1001L)));
    }

    @Test
    void mapsProviderNotFoundFailure() {
        when(userService.getUser(any(GetUserRequest.class))).thenThrow(status(Status.NOT_FOUND, "USER_NOT_FOUND"));

        assertFailure(() -> client.getUser(404L), ExternalDependencyFailure.NOT_FOUND);
    }

    @Test
    void mapsProviderValidationFailure() {
        when(userService.getUser(any(GetUserRequest.class)))
                .thenThrow(status(Status.INVALID_ARGUMENT, "INVALID_USER_ID"));

        assertFailure(() -> client.getUser(0L), ExternalDependencyFailure.VALIDATION_FAILED);
    }

    @Test
    void mapsDubboTimeout() {
        when(userService.getUser(any(GetUserRequest.class))).thenThrow(new RpcException(
                RpcException.TIMEOUT_EXCEPTION, "remote timeout"));

        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.TIMEOUT);
    }

    @Test
    void mapsDubboAvailabilityFailure() {
        when(userService.getUser(any(GetUserRequest.class))).thenThrow(new RpcException(
                RpcException.NETWORK_EXCEPTION, "remote network details"));

        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.UNAVAILABLE);
    }

    @Test
    void rejectsNullProviderResponse() {
        when(userService.getUser(any(GetUserRequest.class))).thenReturn(null);

        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    private static StatusRuntimeException status(Status status, String errorCode) {
        Metadata trailers = new Metadata();
        trailers.put(Metadata.Key.of("x-egon-error-code", Metadata.ASCII_STRING_MARSHALLER), errorCode);
        return status.asRuntimeException(trailers);
    }

    private static void assertFailure(Runnable invocation, ExternalDependencyFailure expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ExternalDependencyException.class, failure -> {
                    assertThat(failure.dependency()).isEqualTo("organization");
                    assertThat(failure.failure()).isEqualTo(expected);
                    assertThat(failure.getMessage()).doesNotContain("remote details");
                });
    }
}
