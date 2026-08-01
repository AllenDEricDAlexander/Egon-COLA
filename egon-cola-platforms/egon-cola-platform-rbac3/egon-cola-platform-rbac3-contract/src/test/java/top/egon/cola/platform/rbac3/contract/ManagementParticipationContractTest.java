package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.management.ManagementPolicyView;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagementParticipationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void managementPolicyKeepsExactJsonFieldsAndRoundTrips()
            throws Exception {
        ManagementPolicyView policy = new ManagementPolicyView(
                "90001",
                "FINANCE_DELEGATED_ADMIN",
                "Finance delegated administration",
                Instant.parse("2026-07-30T00:00:00Z"),
                Instant.parse("2027-07-30T00:00:00Z"),
                List.of(new ManagementPolicyView.Subject(
                        "USER",
                        "10001"
                )),
                List.of(new ManagementPolicyView.Scope(
                        "ORG",
                        "30001"
                )),
                List.of("50001"),
                Set.of("ASSIGN_ROLE"),
                new ManagementPolicyView.Restrictions(
                        30,
                        "HIGH",
                        "MFA",
                        true,
                        true,
                        false,
                        true,
                        Set.of("BUSINESS")
                ),
                7L,
                18L
        );

        JsonNode json = objectMapper.valueToTree(policy);

        assertFields(json, List.of(
                "policyId",
                "policyCode",
                "name",
                "validFrom",
                "validTo",
                "subjects",
                "scopes",
                "activationRootRoleIds",
                "operations",
                "restrictions",
                "version",
                "policyVersion"
        ));
        assertFields(json.path("subjects").get(0), List.of("type", "id"));
        assertFields(json.path("scopes").get(0), List.of("type", "refId"));
        assertFields(json.path("restrictions"), List.of(
                "maxAssignmentDays",
                "maxRiskLevel",
                "requiredAuthStrength",
                "requireReason",
                "requireTicket",
                "includeInheritedSubjectRoles",
                "requireAllAffiliationsInScope",
                "allowedRoleTypes"
        ));
        assertEquals(
                policy,
                objectMapper.treeToValue(json, ManagementPolicyView.class)
        );
    }

    @Test
    void participationCommandKeepsExactJsonFieldsAndRoundTrips()
            throws Exception {
        BusinessParticipationCommand command =
                new BusinessParticipationCommand(
                        "finance-web",
                        "PAYMENT",
                        "payment-9001",
                        "10001",
                        "APPROVE",
                        "event-01",
                        Instant.parse("2026-07-30T08:00:00Z"),
                        "trace-01"
                );

        JsonNode json = objectMapper.valueToTree(command);

        assertFields(json, List.of(
                "applicationCode",
                "businessResource",
                "businessId",
                "actorUserId",
                "actionCode",
                "businessEventId",
                "occurredAt",
                "traceId"
        ));
        assertEquals(
                command,
                objectMapper.treeToValue(
                        json,
                        BusinessParticipationCommand.class
                )
        );
    }

    private static void assertFields(JsonNode json, List<String> expected) {
        List<String> actual = new ArrayList<>();
        json.fieldNames().forEachRemaining(actual::add);
        assertEquals(expected, actual);
    }
}
