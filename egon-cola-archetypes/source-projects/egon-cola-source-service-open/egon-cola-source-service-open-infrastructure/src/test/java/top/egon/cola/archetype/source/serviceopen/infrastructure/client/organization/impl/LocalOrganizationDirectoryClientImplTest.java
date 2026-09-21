package top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import top.egon.cola.archetype.source.serviceopen.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.serviceopen.common.enums.ExternalDependencyFailure;
import org.junit.jupiter.api.Test;

class LocalOrganizationDirectoryClientImplTest {

    private final LocalOrganizationDirectoryClientImpl client = new LocalOrganizationDirectoryClientImpl();

    @Test
    void returnsDeterministicLocalData() {
        assertThat(client.getUser(1001L).name()).isEqualTo("Local User 1001");
        assertThat(client.getSchoolClass(2001L, 3001L).userIds())
                .containsExactly(1001L);
    }

    @Test
    void mapsMissingIdentifiersToNotFound() {
        assertThatThrownBy(() -> client.getUser(0L))
                .isInstanceOfSatisfying(ExternalDependencyException.class,
                        failure -> assertThat(failure.failure())
                                .isEqualTo(ExternalDependencyFailure.NOT_FOUND));
    }
}
