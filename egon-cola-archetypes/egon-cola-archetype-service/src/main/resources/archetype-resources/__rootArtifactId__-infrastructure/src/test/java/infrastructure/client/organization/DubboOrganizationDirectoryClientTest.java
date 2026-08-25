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
        when(userFacade.getUser(1001L)).thenReturn(new UserDetailDTO(
                1001L, "Mario", "m@example.com", "ACTIVE", List.of("STUDENT")));

        assertThat(client.getUser(1001L))
                .isEqualTo(new OrganizationUser(1001L, "Mario", "ACTIVE"));
    }

    @Test
    void mapsOrganizationSchoolClassToConsumerProjection() {
        when(schoolClassFacade.getSchoolClass(2001L, 3001L)).thenReturn(new SchoolClassDetailDTO(
                3001L, "Class One", "G1", "Grade One", "ACTIVE", List.of(1001L)));

        assertThat(client.getSchoolClass(2001L, 3001L)).isEqualTo(new OrganizationSchoolClass(
                3001L, "Class One", "G1", "ACTIVE", List.of(1001L)));
    }

    @Test
    void mapsProviderNotFoundFailure() {
        when(userFacade.getUser(0L)).thenThrow(new OrganizationFacadeException(
                "USER_NOT_FOUND", "remote details", "trace-1"));

        assertFailure(() -> client.getUser(0L), ExternalDependencyFailure.NOT_FOUND);
    }

    @Test
    void mapsProviderValidationFailure() {
        when(userFacade.getUser(-1L)).thenThrow(new OrganizationFacadeException(
                "INVALID_USER_ID", "remote details", "trace-2"));

        assertFailure(() -> client.getUser(-1L), ExternalDependencyFailure.VALIDATION_FAILED);
    }

    @Test
    void mapsDubboTimeout() {
        when(userFacade.getUser(1001L)).thenThrow(new RpcException(
                RpcException.TIMEOUT_EXCEPTION, "remote timeout"));

        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.TIMEOUT);
    }

    @Test
    void mapsDubboAvailabilityFailure() {
        when(userFacade.getUser(1001L)).thenThrow(new RpcException(
                RpcException.NETWORK_EXCEPTION, "remote network details"));

        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.UNAVAILABLE);
    }

    @Test
    void rejectsNullProviderResponse() {
        when(userFacade.getUser(1001L)).thenReturn(null);

        assertFailure(
                () -> client.getUser(1001L),
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
