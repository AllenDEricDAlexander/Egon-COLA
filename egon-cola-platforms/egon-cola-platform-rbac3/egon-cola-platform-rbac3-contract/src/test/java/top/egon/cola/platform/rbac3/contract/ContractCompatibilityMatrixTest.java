package top.egon.cola.platform.rbac3.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.UserAuthorizationSnapshot;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractCompatibilityMatrixTest {

    @Test
    void userAuthorizationSnapshotHasStableStatelessShape() {
        List<String> fields = Arrays.stream(
                        UserAuthorizationSnapshot.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of(
                "systemCode", "tenantId", "identitySub", "rbacUserId",
                "authVersion", "policyVersion", "appContexts", "checksum",
                "generatedAt", "expiresAt"), fields);
    }
}
