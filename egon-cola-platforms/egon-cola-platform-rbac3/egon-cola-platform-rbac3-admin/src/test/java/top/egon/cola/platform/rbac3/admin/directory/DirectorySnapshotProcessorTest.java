package top.egon.cola.platform.rbac3.admin.directory;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.ResolvedUnitVO;

class DirectorySnapshotProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-01T02:00:00Z");
    private final DirectorySnapshotProcessor processor = new DirectorySnapshotProcessor();

    @Test
    void validatesReferencesAndProducesStableTopologicalPaths() {
        var model = processor.validate(Map.of(
                "organizations", List.of(unit("org", "company", null)),
                "departments", List.of(unit("dept", "finance", "org")),
                "positions", List.of(Map.of(
                        "id", "position", "code", "accountant", "name", "Accountant",
                        "orgUnitId", "dept")),
                "userPositions", List.of(Map.of(
                        "userId", "9", "positionId", "position", "primary", true))), NOW);

        assertThat(model.units()).extracting(ResolvedUnitVO::path)
                .containsExactly("/company", "/company/finance");
        assertThat(model.userPositions()).singleElement()
                .satisfies(mapping -> assertThat(mapping.orgUnitId()).isEqualTo("dept"));
        assertThat(model.counts()).containsEntry("departments", 1L)
                .containsEntry("positions", 1);
    }

    @Test
    void rejectsCyclesBeforeAnySnapshotCanBeActivated() {
        assertThatThrownBy(() -> processor.validate(Map.of(
                "organizations", List.of(
                        unit("a", "a-unit", "b"), unit("b", "b-unit", "a"))), NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("DIRECTORY_ORG_CYCLE");
    }

    @Test
    void rejectsPositionAndUserPositionReferenceMismatches() {
        assertThatThrownBy(() -> processor.validate(Map.of(
                "organizations", List.of(unit("org", "company", null)),
                "positions", List.of(Map.of(
                        "id", "position", "code", "accountant", "name", "Accountant",
                        "orgUnitId", "missing"))), NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("DIRECTORY_POSITION_ORG_MISSING");
    }

    @Test
    void rejectsMultiplePrimaryPositionsForOneUser() {
        assertThatThrownBy(() -> processor.validate(Map.of(
                "organizations", List.of(unit("org", "company", null)),
                "positions", List.of(
                        position("first", "first-position", "org"),
                        position("second", "second-position", "org")),
                "userPositions", List.of(
                        Map.of("userId", "9", "positionId", "first", "primary", true),
                        Map.of("userId", "9", "positionId", "second", "primary", true))), NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("DIRECTORY_MULTIPLE_PRIMARY_POSITIONS");
    }

    @Test
    void rejectsInvalidEffectiveTimeWindowDuringStagingValidation() {
        assertThatThrownBy(() -> processor.validate(Map.of(
                "organizations", List.of(Map.of(
                        "id", "org", "code", "company", "name", "Company",
                        "validFrom", "2026-08-02T00:00:00Z",
                        "validTo", "2026-08-01T00:00:00Z"))), NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessageContaining("DIRECTORY_TIME_WINDOW_INVALID");
    }

    private Map<String, Object> position(String id, String code, String orgUnitId) {
        return Map.of(
                "id", id,
                "code", code,
                "name", code,
                "orgUnitId", orgUnitId);
    }

    private Map<String, Object> unit(String id, String code, String parentId) {
        var value = new java.util.LinkedHashMap<String, Object>();
        value.put("id", id);
        value.put("code", code);
        value.put("name", code);
        if (parentId != null) {
            value.put("parentId", parentId);
        }
        return value;
    }
}
