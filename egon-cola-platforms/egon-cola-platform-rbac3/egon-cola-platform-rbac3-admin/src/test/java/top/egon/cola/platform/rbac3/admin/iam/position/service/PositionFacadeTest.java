package top.egon.cola.platform.rbac3.admin.iam.position.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.DirectorySourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.po.PositionPO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

    @Test
    void snapshotPositionCannotBeUpdatedManually() {
        PositionPO position = new PositionPO(
                11L, 7L, DirectorySourceTypeEnum.DIRECTORY_SNAPSHOT, 101L,
                "accountant", "Accountant", 10L, null, NOW, null,
                "actor", NOW);

        assertThatThrownBy(() -> position.updateManually(
                "Accountant 2", 10L, null, NOW, null,
                0L, "actor", NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}
