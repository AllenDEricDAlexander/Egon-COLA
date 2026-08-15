package top.egon.cola.platform.rbac3.admin.iam.organization.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.DirectorySourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

    @Test
    void manualOrganizationAcceptsManualUpdate() {
        OrgUnitPO organization = new OrgUnitPO(
                10L, 7L, DirectorySourceTypeEnum.MANUAL, null,
                OrgUnitUnitTypeEnum.DEPT, "finance", "Finance", null,
                "finance", 0, null, NOW, null, "actor", NOW);

        assertThatCode(() -> organization.updateManually(
                OrgUnitUnitTypeEnum.DEPT, "Finance 2", null,
                "finance", 0, null, NOW, null, 0L, "actor", NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void snapshotOrganizationCannotBeUpdatedManually() {
        OrgUnitPO organization = new OrgUnitPO(
                10L, 7L, DirectorySourceTypeEnum.DIRECTORY_SNAPSHOT, 100L,
                OrgUnitUnitTypeEnum.DEPT, "finance", "Finance", null,
                "finance", 0, null, NOW, null, "actor", NOW);

        assertThatThrownBy(() -> organization.updateManually(
                OrgUnitUnitTypeEnum.DEPT, "Finance 2", null,
                "finance", 0, null, NOW, null, 0L, "actor", NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}
