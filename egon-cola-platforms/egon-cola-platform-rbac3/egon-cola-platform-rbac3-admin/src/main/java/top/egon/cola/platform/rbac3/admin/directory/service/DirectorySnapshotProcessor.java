package top.egon.cola.platform.rbac3.admin.directory.service;

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
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.SnapshotModelVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.ResolvedUnitVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.PositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.UserPositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.UnitInputDTO;

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
    public SnapshotModelVO validate(Map<String, Object> payload, Instant generatedAt) {
        Map<String, UnitInputDTO> units = new LinkedHashMap<>();
        Set<String> unitCodes = new HashSet<>();
        readUnits(payload, "organizations", "ORG", generatedAt, units, unitCodes);
        readUnits(payload, "departments", "DEPT", generatedAt, units, unitCodes);

        Map<String, Integer> depths = new HashMap<>();
        Map<String, String> paths = new HashMap<>();
        Set<String> visiting = new HashSet<>();
        for (UnitInputDTO unit : units.values()) {
            resolveUnit(unit.id(), units, visiting, depths, paths);
        }

        List<ResolvedUnitVO> resolvedUnits = units.values().stream()
                .map(unit -> new ResolvedUnitVO(
                        unit.id(), unit.type(), unit.code(), unit.name(), unit.parentId(),
                        paths.get(unit.id()), depths.get(unit.id()), unit.externalId(),
                        unit.validFrom(), unit.validTo()))
                .sorted(Comparator.comparingInt(ResolvedUnitVO::depth)
                        .thenComparing(ResolvedUnitVO::code))
                .toList();

        Map<String, PositionInputDTO> positions = readPositions(
                payload, generatedAt, units);
        List<UserPositionInputDTO> userPositions = readUserPositions(
                payload, generatedAt, units, positions);
        Map<String, Object> counts = Map.of(
                "organizations", countType(resolvedUnits, "ORG"),
                "departments", countType(resolvedUnits, "DEPT"),
                "positions", positions.size(),
                "userPositions", userPositions.size());
        return new SnapshotModelVO(
                resolvedUnits,
                positions.values().stream().sorted(
                        Comparator.comparing(PositionInputDTO::code)).toList(),
                userPositions.stream().sorted(
                        Comparator.comparing(UserPositionInputDTO::userId)
                                .thenComparing(UserPositionInputDTO::positionId))
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
            Map<String, UnitInputDTO> units,
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
            units.put(id, new UnitInputDTO(
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
            Map<String, UnitInputDTO> units,
            Set<String> visiting,
            Map<String, Integer> depths,
            Map<String, String> paths) {
        Integer resolved = depths.get(id);
        if (resolved != null) {
            return resolved;
        }
        UnitInputDTO unit = units.get(id);
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
            UnitInputDTO parent = units.get(unit.parentId());
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
    private Map<String, PositionInputDTO> readPositions(
            Map<String, Object> payload,
            Instant generatedAt,
            Map<String, UnitInputDTO> units) {
        Map<String, PositionInputDTO> positions = new LinkedHashMap<>();
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
            positions.put(id, new PositionInputDTO(
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
    private List<UserPositionInputDTO> readUserPositions(
            Map<String, Object> payload,
            Instant generatedAt,
            Map<String, UnitInputDTO> units,
            Map<String, PositionInputDTO> positions) {
        List<UserPositionInputDTO> result = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        Set<String> primaryUsers = new HashSet<>();
        for (Map<String, Object> value : objects(payload, "userPositions")) {
            String userId = required(value, "userId");
            String positionId = required(value, "positionId");
            PositionInputDTO position = positions.get(positionId);
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
            result.add(new UserPositionInputDTO(
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
    private long countType(List<ResolvedUnitVO> units, String type) {
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





    }
