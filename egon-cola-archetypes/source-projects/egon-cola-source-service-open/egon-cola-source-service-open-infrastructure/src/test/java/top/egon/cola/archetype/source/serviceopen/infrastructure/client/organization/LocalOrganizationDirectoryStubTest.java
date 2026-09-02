package top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import top.egon.cola.archetype.source.serviceopen.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.serviceopen.domain.client.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalOrganizationDirectoryStubTest {

    private final LocalOrganizationDirectoryStub stub = new LocalOrganizationDirectoryStub();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(stub.getUser(1001L).name()).isEqualTo("Local User 1001");
        assertThat(stub.getSchoolClass(3001L, 2001L).userIds())
                .containsExactly(2001L);
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> stub.getUser(404L))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
