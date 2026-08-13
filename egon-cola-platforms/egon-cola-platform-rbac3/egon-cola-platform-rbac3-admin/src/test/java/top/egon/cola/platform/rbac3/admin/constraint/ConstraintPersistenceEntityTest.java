package top.egon.cola.platform.rbac3.admin.constraint;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.DataRulePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.FieldRulePO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.OperationSodRulePO;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.FieldRuleAccessLevelEnum;

class ConstraintPersistenceEntityTest {

    @Test
    void typedRulesRejectInvalidWindowsAndExposeStableFacts() {
        Instant start = Instant.parse("2026-07-30T00:00:00Z");
        DataRulePO dataRule = new DataRulePO(
                1L, 10L, 20L, 30L, 40L,
                DataRuleScopeTypeEnum.DEPT_TREE, 5L,
                start, null, "actor", start);
        FieldRulePO fieldRule = new FieldRulePO(
                2L, 10L, 20L, 30L, 40L, 50L,
                FieldRuleAccessLevelEnum.MASKED_READ,
                start, null, "actor", start);

        assertEquals(DataRuleScopeTypeEnum.DEPT_TREE, dataRule.getScopeType());
        assertEquals(FieldRuleAccessLevelEnum.MASKED_READ, fieldRule.getAccessLevel());
        assertThrows(IllegalArgumentException.class,
                () -> new OperationSodRulePO(
                        3L, 10L, "finance", "payment", "approve", "approve",
                        null, start, null, "actor", start));
    }
}
