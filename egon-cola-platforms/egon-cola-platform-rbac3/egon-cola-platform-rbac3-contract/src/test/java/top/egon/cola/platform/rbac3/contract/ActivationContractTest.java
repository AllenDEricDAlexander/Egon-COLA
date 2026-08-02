package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void replacementResultIsSeparateFromActiveRoleViewAndRoundTrips()
            throws Exception {
        List<ActiveRoleSetView.ApplicationActiveRoles> roles =
                new ArrayList<>(List.of(applicationRoles()));
        ReplaceActiveRolesResult result = new ReplaceActiveRolesResult(
                roles,
                true,
                4L,
                6L,
                9L,
                false,
                "sha256:snapshot"
        );
        roles.clear();

        JsonNode json = objectMapper.valueToTree(result);

        assertEquals(Set.of(
                "activeRoles",
                "changed",
                "contextVersion",
                "authVersion",
                "policyVersion",
                "bootstrapRequired",
                "snapshotChecksum"
        ), fieldNames(json));
        assertEquals(
                result,
                objectMapper.treeToValue(json, ReplaceActiveRolesResult.class)
        );
        assertEquals(1, result.activeRoles().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.activeRoles().clear()
        );
        assertFalse(result.toString().contains("accessToken"));
    }

    @Test
    void replacementResultRejectsInvalidVersionsAndToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceActiveRolesResult(
                        List.of(applicationRoles()),
                        true,
                        -1L,
                        1L,
                        1L,
                        false,
                        "sha256:snapshot"
                )
        );
    }

    @Test
    void replacementRequestCanonicalizesDecimalRoleIds() {
        ReplaceActiveRolesRequest request = new ReplaceActiveRolesRequest(
                List.of(" 50001 ", "00051001"),
                3L
        );

        assertEquals(List.of("50001", "51001"), request.roleIds());
    }

    @Test
    void replacementRequestRejectsInvalidAndNormalizedDuplicateRoleIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceActiveRolesRequest(
                        List.of("role-50001"),
                        3L
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceActiveRolesRequest(
                        List.of("50001", " 050001 "),
                        3L
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceActiveRolesRequest(List.of("0"), 3L)
        );
    }

    @Test
    void activeRoleViewKeepsItsExactGetFieldsAndRoundTrips()
            throws Exception {
        ActiveRoleSetView view = new ActiveRoleSetView(
                "40001",
                List.of(applicationRoles()),
                false,
                43L,
                3L,
                18L,
                "sha256:snapshot"
        );

        JsonNode json = objectMapper.valueToTree(view);

        assertEquals(Set.of(
                "sessionId",
                "activeRoles",
                "activationRequired",
                "authVersion",
                "sessionVersion",
                "policyVersion",
                "snapshotChecksum"
        ), fieldNames(json));
        assertEquals(Set.of(
                "applicationCode",
                "rootRoleIds"
        ), fieldNames(json.path("activeRoles").get(0)));
        assertEquals(
                view,
                objectMapper.treeToValue(json, ActiveRoleSetView.class)
        );
    }

    private static ActiveRoleSetView.ApplicationActiveRoles applicationRoles() {
        return new ActiveRoleSetView.ApplicationActiveRoles(
                "finance-web",
                List.of("50001", "51001")
        );
    }

    private static Set<String> fieldNames(JsonNode node) {
        return node.properties().stream()
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
