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

/**
 * 类型 `DirectorySnapshotProcessor` 位于当前包内，是类型，用于承载 `Directory Snapshot Processor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DirectorySnapshotProcessor` is a type in its package and carries the responsibility, state, or contract for `Directory Snapshot Processor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Adapts an immutable provider payload into a validated, topologically ordered model.
 */
public final class DirectorySnapshotProcessor {

    /**
     * 字段 `MAX_DEPTH` 表示 `DirectorySnapshotProcessor` 中与 `MAX DEPTH` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_DEPTH` stores the `MAX DEPTH`-related state, dependency, configuration, or result of `DirectorySnapshotProcessor` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_DEPTH` 时应保持 `DirectorySnapshotProcessor` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_DEPTH`, preserve `DirectorySnapshotProcessor`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final int MAX_DEPTH = 20;

    /**
     * 方法 `validate` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `readUnits` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `read Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `readUnits` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `read Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `readUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `readUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param codes 输入参数 `codes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `resolveUnit` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `resolve Unit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolveUnit` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `resolve Unit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resolveUnit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resolveUnit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param visiting 输入参数 `visiting`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param depths 输入参数 `depths`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param paths 输入参数 `paths`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `readPositions` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `read Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `readPositions` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `read Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `readPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `readPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `readUserPositions` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `read User Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `readUserPositions` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `read User Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `readUserPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `readUserPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generatedAt 输入参数 `generatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positions 输入参数 `positions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `objects` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `objects` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `objects` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `objects` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `objects` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `objects`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `required` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(Map<String, Object> source, String field) {
        String value = optional(source, field);
        if (value == null) {
            throw violation("DIRECTORY_PAYLOAD_INVALID");
        }
        return value;
    }

    /**
     * 方法 `optional` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `optional` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `optional` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `optional` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `optional` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `optional`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String optional(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 方法 `instant` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `instant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `instant` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `instant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `instant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `instant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param defaultValue 输入参数 `defaultValue`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Instant instant(
            Map<String, Object> source,
            String field,
            Instant defaultValue) {
        Instant value = nullableInstant(source, field);
        return value == null ? defaultValue : value;
    }

    /**
     * 方法 `nullableInstant` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `nullable Instant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nullableInstant` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `nullable Instant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nullableInstant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nullableInstant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `bool` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `bool` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bool` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `bool` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bool` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bool`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param defaultValue 输入参数 `defaultValue`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `validateWindow` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `validate Window` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateWindow` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `validate Window` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateWindow` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateWindow`, then continue the business flow using its result, exception, or side effect.
     *
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateWindow(Instant validFrom, Instant validTo) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw violation("DIRECTORY_TIME_WINDOW_INVALID");
        }
    }

    /**
     * 方法 `countType` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `count Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `countType` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `count Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `countType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `countType`, then continue the business flow using its result, exception, or side effect.
     *
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long countType(List<ResolvedUnit> units, String type) {
        return units.stream().filter(unit -> type.equals(unit.type())).count();
    }

    /**
     * 方法 `violation` 按照 `DirectorySnapshotProcessor` 的职责处理输入，完成 `violation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `violation` processes its inputs according to `DirectorySnapshotProcessor`'s responsibility, performs the `violation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `violation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `violation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Rbac3RuleViolation violation(String code) {
        return new Rbac3RuleViolation(code);
    }

    /**
     * 类型 `SnapshotModel` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `Snapshot Model` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SnapshotModel` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `Snapshot Model`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SnapshotModel` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotModel` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param units 记录组件 `units` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `units` carries constructor data whose meaning is defined by the record contract.
     * @param positions 记录组件 `positions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positions` carries constructor data whose meaning is defined by the record contract.
     * @param userPositions 记录组件 `userPositions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userPositions` carries constructor data whose meaning is defined by the record contract.
     * @param counts 记录组件 `counts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `counts` carries constructor data whose meaning is defined by the record contract.
     */
    public record SnapshotModel(
            /**
             * 字段 `units` 表示 `SnapshotModel` 中与 `units` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ResolvedUnit&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `units` stores the `units`-related state, dependency, configuration, or result of `SnapshotModel` (declared type `List&lt;ResolvedUnit&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `units` 时应保持 `SnapshotModel` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `units`, preserve `SnapshotModel`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ResolvedUnit> units,
            /**
             * 字段 `positions` 表示 `SnapshotModel` 中与 `positions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;PositionInput&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `positions` stores the `positions`-related state, dependency, configuration, or result of `SnapshotModel` (declared type `List&lt;PositionInput&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `positions` 时应保持 `SnapshotModel` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `positions`, preserve `SnapshotModel`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<PositionInput> positions,
            /**
             * 字段 `userPositions` 表示 `SnapshotModel` 中与 `user Positions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;UserPositionInput&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userPositions` stores the `user Positions`-related state, dependency, configuration, or result of `SnapshotModel` (declared type `List&lt;UserPositionInput&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userPositions` 时应保持 `SnapshotModel` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userPositions`, preserve `SnapshotModel`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<UserPositionInput> userPositions,
            /**
             * 字段 `counts` 表示 `SnapshotModel` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `SnapshotModel` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `counts` 时应保持 `SnapshotModel` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `counts`, preserve `SnapshotModel`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Object> counts) {

        /**
         * 构造器 `SnapshotModel` 用于创建并初始化 `SnapshotModel` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SnapshotModel` creates and initializes `SnapshotModel`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SnapshotModel` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SnapshotModel`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param positions 输入参数 `positions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userPositions 输入参数 `userPositions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param counts 输入参数 `counts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SnapshotModel {
            units = List.copyOf(units);
            positions = List.copyOf(positions);
            userPositions = List.copyOf(userPositions);
            counts = Map.copyOf(counts);
        }
    }

    /**
     * 类型 `ResolvedUnit` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `Resolved Unit` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolvedUnit` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `Resolved Unit`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolvedUnit` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolvedUnit` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param parentId 记录组件 `parentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `parentId` carries constructor data whose meaning is defined by the record contract.
     * @param path 记录组件 `path` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `path` carries constructor data whose meaning is defined by the record contract.
     * @param depth 记录组件 `depth` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `depth` carries constructor data whose meaning is defined by the record contract.
     * @param externalId 记录组件 `externalId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolvedUnit(
            /**
             * 字段 `id` 表示 `ResolvedUnit` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `id` stores the `id`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `id` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `id`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String id,
            /**
             * 字段 `type` 表示 `ResolvedUnit` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `type` stores the `type`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `type` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `type`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String type,
            /**
             * 字段 `code` 表示 `ResolvedUnit` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String code,
            /**
             * 字段 `name` 表示 `ResolvedUnit` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `parentId` 表示 `ResolvedUnit` 中与 `parent Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `parentId` stores the `parent Id`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `parentId` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `parentId`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String parentId,
            /**
             * 字段 `path` 表示 `ResolvedUnit` 中与 `path` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `path` stores the `path`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `path` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `path`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String path,
            /**
             * 字段 `depth` 表示 `ResolvedUnit` 中与 `depth` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `depth` stores the `depth`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `depth` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `depth`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            int depth,
            /**
             * 字段 `externalId` 表示 `ResolvedUnit` 中与 `external Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalId` stores the `external Id`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalId` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalId`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            String externalId,
            /**
             * 字段 `validFrom` 表示 `ResolvedUnit` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `ResolvedUnit` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `ResolvedUnit` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `ResolvedUnit` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `ResolvedUnit`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo) {
    }

    /**
     * 类型 `PositionInput` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `Position Input` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PositionInput` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `Position Input`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PositionInput` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PositionInput` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param orgUnitId 记录组件 `orgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `orgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param externalId 记录组件 `externalId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     */
    public record PositionInput(
            /**
             * 字段 `id` 表示 `PositionInput` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `id` stores the `id`-related state, dependency, configuration, or result of `PositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `id` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `id`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String id,
            /**
             * 字段 `code` 表示 `PositionInput` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `PositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String code,
            /**
             * 字段 `name` 表示 `PositionInput` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `PositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `orgUnitId` 表示 `PositionInput` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `PositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String orgUnitId,
            /**
             * 字段 `externalId` 表示 `PositionInput` 中与 `external Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalId` stores the `external Id`-related state, dependency, configuration, or result of `PositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalId` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalId`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String externalId,
            /**
             * 字段 `validFrom` 表示 `PositionInput` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `PositionInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `PositionInput` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `PositionInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `PositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `PositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo) {
    }

    /**
     * 类型 `UserPositionInput` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `User Position Input` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UserPositionInput` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `User Position Input`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UserPositionInput` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UserPositionInput` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param positionId 记录组件 `positionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positionId` carries constructor data whose meaning is defined by the record contract.
     * @param orgUnitId 记录组件 `orgUnitId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `orgUnitId` carries constructor data whose meaning is defined by the record contract.
     * @param primary 记录组件 `primary` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `primary` carries constructor data whose meaning is defined by the record contract.
     * @param externalAssignmentId 记录组件 `externalAssignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalAssignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     */
    public record UserPositionInput(
            /**
             * 字段 `userId` 表示 `UserPositionInput` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `positionId` 表示 `UserPositionInput` 中与 `position Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `positionId` stores the `position Id`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `positionId` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `positionId`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String positionId,
            /**
             * 字段 `orgUnitId` 表示 `UserPositionInput` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String orgUnitId,
            /**
             * 字段 `primary` 表示 `UserPositionInput` 中与 `primary` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `primary` stores the `primary`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `primary` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `primary`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean primary,
            /**
             * 字段 `externalAssignmentId` 表示 `UserPositionInput` 中与 `external Assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalAssignmentId` stores the `external Assignment Id`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalAssignmentId` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalAssignmentId`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String externalAssignmentId,
            /**
             * 字段 `validFrom` 表示 `UserPositionInput` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `UserPositionInput` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `UserPositionInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `UserPositionInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `UserPositionInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo) {
    }

    /**
     * 类型 `UnitInput` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `Unit Input` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UnitInput` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `Unit Input`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UnitInput` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UnitInput` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param parentId 记录组件 `parentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `parentId` carries constructor data whose meaning is defined by the record contract.
     * @param externalId 记录组件 `externalId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     */
    private record UnitInput(
            /**
             * 字段 `id` 表示 `UnitInput` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `id` stores the `id`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `id` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `id`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String id,
            /**
             * 字段 `type` 表示 `UnitInput` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `type` stores the `type`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `type` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `type`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String type,
            /**
             * 字段 `code` 表示 `UnitInput` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `code` stores the `code`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `code` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `code`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String code,
            /**
             * 字段 `name` 表示 `UnitInput` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `parentId` 表示 `UnitInput` 中与 `parent Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `parentId` stores the `parent Id`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `parentId` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `parentId`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String parentId,
            /**
             * 字段 `externalId` 表示 `UnitInput` 中与 `external Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalId` stores the `external Id`-related state, dependency, configuration, or result of `UnitInput` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalId` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalId`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            String externalId,
            /**
             * 字段 `validFrom` 表示 `UnitInput` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `UnitInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `UnitInput` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `UnitInput` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `UnitInput` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `UnitInput`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo) {
    }
}
