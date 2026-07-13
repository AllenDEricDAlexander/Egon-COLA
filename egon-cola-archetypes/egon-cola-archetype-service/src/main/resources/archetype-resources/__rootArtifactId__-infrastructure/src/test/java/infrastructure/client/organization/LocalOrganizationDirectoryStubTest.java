#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalOrganizationDirectoryStubTest {

    private final LocalOrganizationDirectoryStub stub = new LocalOrganizationDirectoryStub();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(stub.getUser("user-1").name()).isEqualTo("Local User user-1");
        assertThat(stub.getSchoolClass("class-1").userIds()).containsExactly("local-user");
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> stub.getUser("missing-user"))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
