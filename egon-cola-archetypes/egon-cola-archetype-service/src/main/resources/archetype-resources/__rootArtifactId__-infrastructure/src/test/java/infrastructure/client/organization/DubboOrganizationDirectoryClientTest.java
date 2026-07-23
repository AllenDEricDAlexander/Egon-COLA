#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import top.egon.cola.organization.facade.teaching.SchoolClassFacade;
import top.egon.cola.organization.facade.user.UserFacade;
import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.organization.OrganizationSchoolClass;
import ${package}.domain.client.organization.OrganizationUser;
import java.util.List;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DubboOrganizationDirectoryClientTest {

    private UserFacade userFacade;
    private SchoolClassFacade schoolClassFacade;
    private DubboOrganizationDirectoryClient client;

    @BeforeEach
    void setUp() {
        userFacade = mock(UserFacade.class);
        schoolClassFacade = mock(SchoolClassFacade.class);
        client = new DubboOrganizationDirectoryClient(userFacade, schoolClassFacade);
    }

    @Test
    void mapsOrganizationUserToConsumerProjection() {
        when(userFacade.getUser("user-1")).thenReturn(new UserDetailDTO(
                "user-1", "Mario", "m@example.com", "ACTIVE", List.of("STUDENT")));

        assertThat(client.getUser("user-1"))
                .isEqualTo(new OrganizationUser("user-1", "Mario", "ACTIVE"));
    }

    @Test
    void mapsOrganizationSchoolClassToConsumerProjection() {
        when(schoolClassFacade.getSchoolClass("class-1")).thenReturn(new SchoolClassDetailDTO(
                "class-1", "Class One", "G1", "Grade One", "ACTIVE", List.of("user-1")));

        assertThat(client.getSchoolClass("class-1")).isEqualTo(new OrganizationSchoolClass(
                "class-1", "Class One", "G1", "ACTIVE", List.of("user-1")));
    }

    @Test
    void mapsProviderNotFoundFailure() {
        when(userFacade.getUser("missing-user")).thenThrow(new OrganizationFacadeException(
                "USER_NOT_FOUND", "remote details", "trace-1"));

        assertFailure(() -> client.getUser("missing-user"), ExternalDependencyFailure.NOT_FOUND);
    }

    @Test
    void mapsProviderValidationFailure() {
        when(userFacade.getUser("invalid-user")).thenThrow(new OrganizationFacadeException(
                "INVALID_USER_ID", "remote details", "trace-2"));

        assertFailure(() -> client.getUser("invalid-user"), ExternalDependencyFailure.VALIDATION_FAILED);
    }

    @Test
    void mapsDubboTimeout() {
        when(userFacade.getUser("user-1")).thenThrow(new RpcException(
                RpcException.TIMEOUT_EXCEPTION, "remote timeout"));

        assertFailure(() -> client.getUser("user-1"), ExternalDependencyFailure.TIMEOUT);
    }

    @Test
    void mapsDubboAvailabilityFailure() {
        when(userFacade.getUser("user-1")).thenThrow(new RpcException(
                RpcException.NETWORK_EXCEPTION, "remote network details"));

        assertFailure(() -> client.getUser("user-1"), ExternalDependencyFailure.UNAVAILABLE);
    }

    @Test
    void rejectsNullProviderResponse() {
        when(userFacade.getUser("user-1")).thenReturn(null);

        assertFailure(
                () -> client.getUser("user-1"),
                ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
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
