package top.egon.cola.platform.rbac3.admin.directory.application;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapts an immutable provider payload into a validated, topologically ordered model. */
public final class DirectorySnapshotProcessor {

    private static final int MAX_DEPTH = 20;

    public SnapshotModel validate(Map<String, Object> payload, Instant generatedAt) {
        Map<String, UnitInput> units = new LinkedHashMap<>();
        Set<String> unitCodes = new HashSet<>();
        readUnits(payload, "organizations", "ORG", generatedAt, units, unitCodes);
        readUnits(payload, "departments", "DEPT", generatedAt, units, unitCodes);

        Map<String, Integer> depths = new HashMap<>();
        Map<String, String> paths = new HashMap<>();
        Set<String> visiting = new HashSet<>();
        for (UnitInput unit : units.values()) {
            resolveUnit(unit.id(), units, visiting, depths, paths);
        }

        List<ResolvedUnit> resolvedUnits = units.values().stream()
                .map(unit -> new ResolvedUnit(
                        unit.id(), unit.type(), unit.code(), unit.name(), unit.parentId(),
                        paths.get(unit.id()), depths.get(unit.id()), unit.externalId(),
                        unit.validFrom(), unit.validTo()))
                .sorted(Comparator.comparingInt(ResolvedUnit::depth)
                        .thenComparing(ResolvedUnit::code))
                .toList();

        Map<String, PositionInput> positions = readPositions(
                payload, generatedAt, units);
        List<UserPositionInput> userPositions = readUserPositions(
                payload, generatedAt, units, positions);
        Map<String, Object> counts = Map.of(
                "organizations", countType(resolvedUnits, "ORG"),
                "departments", countType(resolvedUnits, "DEPT"),
                "positions", positions.size(),
                "userPositions", userPositions.size());
        return new SnapshotModel(
                resolvedUnits,
                positions.values().stream().sorted(
                        Comparator.comparing(PositionInput::code)).toList(),
                userPositions.stream().sorted(
                        Comparator.comparing(UserPositionInput::userId)
                                .thenComparing(UserPositionInput::positionId))
                        .toList(),
                counts);
    }

    private void readUnits(
            Map<String, Object> payload,
            String field,
            String type,
            Instant generatedAt,
            Map<String, UnitInput> units,
            Set<String> codes) {
        for (Map<String, Object> value : objects(payload, field)) {
            String id = required(value, "id");
            String code = required(value, "code");
            if (units.containsKey(id) || !codes.add(code)) {
                throw violation("DIRECTORY_DUPLICATE_UNIT");
            }
            Instant validFrom = instant(value, "validFrom", generatedAt);
            Instant validTo = nullableInstant(value, "validTo");
            validateWindow(validFrom, validTo);
            units.put(id, new UnitInput(
                    id, type, code, required(value, "name"), optional(value, "parentId"),
                    optional(value, "externalId"), validFrom, validTo));
        }
    }

    private int resolveUnit(
            String id,
            Map<String, UnitInput> units,
            Set<String> visiting,
            Map<String, Integer> depths,
            Map<String, String> paths) {
        Integer resolved = depths.get(id);
        if (resolved != null) {
            return resolved;
        }
        UnitInput unit = units.get(id);
        if (unit == null) {
            throw violation("DIRECTORY_ORG_REFERENCE_MISSING");
        }
        if (!visiting.add(id)) {
            throw violation("DIRECTORY_ORG_CYCLE");
        }
        int depth;
        String path;
        if (unit.parentId() == null) {
            depth = 0;
            path = '/' + unit.code();
        } else {
            UnitInput parent = units.get(unit.parentId());
            if (parent == null) {
                throw violation("DIRECTORY_ORG_REFERENCE_MISSING");
            }
            depth = Math.addExact(resolveUnit(
                    parent.id(), units, visiting, depths, paths), 1);
            path = paths.get(parent.id()) + '/' + unit.code();
        }
        visiting.remove(id);
        if (depth > MAX_DEPTH) {
            throw violation("DIRECTORY_ORG_DEPTH_EXCEEDED");
        }
        depths.put(id, depth);
        paths.put(id, path);
        return depth;
    }

    private Map<String, PositionInput> readPositions(
            Map<String, Object> payload,
            Instant generatedAt,
            Map<String, UnitInput> units) {
        Map<String, PositionInput> positions = new LinkedHashMap<>();
        Set<String> codes = new HashSet<>();
        for (Map<String, Object> value : objects(payload, "positions")) {
            String id = required(value, "id");
            String code = required(value, "code");
            String orgUnitId = required(value, "orgUnitId");
            if (positions.containsKey(id) || !codes.add(code)) {
                throw violation("DIRECTORY_DUPLICATE_POSITION");
            }
            if (!units.containsKey(orgUnitId)) {
                throw violation("DIRECTORY_POSITION_ORG_MISSING");
            }
            Instant validFrom = instant(value, "validFrom", generatedAt);
            Instant validTo = nullableInstant(value, "validTo");
            validateWindow(validFrom, validTo);
            positions.put(id, new PositionInput(
                    id, code, required(value, "name"), orgUnitId,
                    optional(value, "externalId"), validFrom, validTo));
        }
        return positions;
    }

    private List<UserPositionInput> readUserPositions(
            Map<String, Object> payload,
            Instant generatedAt,
            Map<String, UnitInput> units,
            Map<String, PositionInput> positions) {
        List<UserPositionInput> result = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        Set<String> primaryUsers = new HashSet<>();
        for (Map<String, Object> value : objects(payload, "userPositions")) {
            String userId = required(value, "userId");
            String positionId = required(value, "positionId");
            PositionInput position = positions.get(positionId);
            if (position == null) {
                throw violation("DIRECTORY_USER_POSITION_MISSING");
            }
            String orgUnitId = optional(value, "orgUnitId");
            if (orgUnitId == null) {
                orgUnitId = position.orgUnitId();
            }
            if (!units.containsKey(orgUnitId) || !position.orgUnitId().equals(orgUnitId)) {
                throw violation("DIRECTORY_USER_POSITION_ORG_MISMATCH");
            }
            Instant validFrom = instant(value, "validFrom", generatedAt);
            Instant validTo = nullableInstant(value, "validTo");
            validateWindow(validFrom, validTo);
            String identity = userId + '|' + positionId + '|' + validFrom;
            if (!identities.add(identity)) {
                throw violation("DIRECTORY_DUPLICATE_USER_POSITION");
            }
            boolean primary = bool(value, "primary", false);
            if (primary && !primaryUsers.add(userId)) {
                throw violation("DIRECTORY_MULTIPLE_PRIMARY_POSITIONS");
            }
            result.add(new UserPositionInput(
                    userId, positionId, orgUnitId, primary,
                    optional(value, "externalAssignmentId"), validFrom,
                    validTo));
        }
        return result;
    }

    private List<Map<String, Object>> objects(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof Collection<?> collection)) {
            throw violation("DIRECTORY_PAYLOAD_INVALID");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : collection) {
            if (!(item instanceof Map<?, ?> map)) {
                throw violation("DIRECTORY_PAYLOAD_INVALID");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, element) -> normalized.put(String.valueOf(key), element));
            result.add(normalized);
        }
        return result;
    }

    private String required(Map<String, Object> source, String field) {
        String value = optional(source, field);
        if (value == null) {
            throw violation("DIRECTORY_PAYLOAD_INVALID");
        }
        return value;
    }

    private String optional(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Instant instant(
            Map<String, Object> source,
            String field,
            Instant defaultValue) {
        Instant value = nullableInstant(source, field);
        return value == null ? defaultValue : value;
    }

    private Instant nullableInstant(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        try {
            return value instanceof Instant instant
                    ? instant : Instant.parse(String.valueOf(value));
        } catch (DateTimeParseException exception) {
            throw violation("DIRECTORY_TIME_INVALID");
        }
    }

    private boolean bool(Map<String, Object> source, String field, boolean defaultValue) {
        Object value = source.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if ("true".equalsIgnoreCase(String.valueOf(value))) {
            return true;
        }
        if ("false".equalsIgnoreCase(String.valueOf(value))) {
            return false;
        }
        throw violation("DIRECTORY_PAYLOAD_INVALID");
    }

    private void validateWindow(Instant validFrom, Instant validTo) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw violation("DIRECTORY_TIME_WINDOW_INVALID");
        }
    }

    private long countType(List<ResolvedUnit> units, String type) {
        return units.stream().filter(unit -> type.equals(unit.type())).count();
    }

    private Rbac3RuleViolation violation(String code) {
        return new Rbac3RuleViolation(code);
    }

    public record SnapshotModel(
            List<ResolvedUnit> units,
            List<PositionInput> positions,
            List<UserPositionInput> userPositions,
            Map<String, Object> counts) {

        public SnapshotModel {
            units = List.copyOf(units);
            positions = List.copyOf(positions);
            userPositions = List.copyOf(userPositions);
            counts = Map.copyOf(counts);
        }
    }

    public record ResolvedUnit(
            String id,
            String type,
            String code,
            String name,
            String parentId,
            String path,
            int depth,
            String externalId,
            Instant validFrom,
            Instant validTo) {
    }

    public record PositionInput(
            String id,
            String code,
            String name,
            String orgUnitId,
            String externalId,
            Instant validFrom,
            Instant validTo) {
    }

    public record UserPositionInput(
            String userId,
            String positionId,
            String orgUnitId,
            boolean primary,
            String externalAssignmentId,
            Instant validFrom,
            Instant validTo) {
    }

    private record UnitInput(
            String id,
            String type,
            String code,
            String name,
            String parentId,
            String externalId,
            Instant validFrom,
            Instant validTo) {
    }
}
