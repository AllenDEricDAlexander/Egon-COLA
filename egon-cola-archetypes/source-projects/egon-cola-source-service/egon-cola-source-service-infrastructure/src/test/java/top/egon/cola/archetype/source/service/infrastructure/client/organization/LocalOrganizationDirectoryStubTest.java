package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalOrganizationDirectoryStubTest {

    private final LocalOrganizationDirectoryStub stub = new LocalOrganizationDirectoryStub();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(stub.getUser(1001L).name()).isEqualTo("Local User 1001");
        assertThat(stub.getSchoolClass(2001L, 3001L).userIds())
                .containsExactly(1001L);
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> stub.getUser(0L))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
