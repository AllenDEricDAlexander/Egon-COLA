package top.egon.cola.platform.rbac3.admin.constraint;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.constraint.domain.DataRuleEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.FieldRuleEntity;
import top.egon.cola.platform.rbac3.admin.constraint.domain.OperationSodRuleEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintPersistenceEntityTest {

    @Test
    void typedRulesRejectInvalidWindowsAndExposeStableFacts() {
        Instant start = Instant.parse("2026-07-30T00:00:00Z");
        DataRuleEntity dataRule = new DataRuleEntity(
                1L, 10L, 20L, 30L, 40L,
                DataRuleEntity.ScopeType.DEPT_TREE, 5L,
                start, null, "actor", start);
        FieldRuleEntity fieldRule = new FieldRuleEntity(
                2L, 10L, 20L, 30L, 40L, 50L,
                FieldRuleEntity.AccessLevel.MASKED_READ,
                start, null, "actor", start);

        assertEquals(DataRuleEntity.ScopeType.DEPT_TREE, dataRule.getScopeType());
        assertEquals(FieldRuleEntity.AccessLevel.MASKED_READ, fieldRule.getAccessLevel());
        assertThrows(IllegalArgumentException.class,
                () -> new OperationSodRuleEntity(
                        3L, 10L, "finance", "payment", "approve", "approve",
                        null, start, null, "actor", start));
    }
}
