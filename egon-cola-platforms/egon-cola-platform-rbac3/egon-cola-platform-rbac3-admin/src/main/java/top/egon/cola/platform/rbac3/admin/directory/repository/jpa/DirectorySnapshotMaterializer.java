package top.egon.cola.platform.rbac3.admin.directory.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.UserPositionSnapshotPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.MaterializationResultVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.UserPositionKey;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.SnapshotModelVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.ResolvedUnitVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.PositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.UserPositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.UserPositionSnapshotStatusEnum;

/**
 * 类型 `DirectorySnapshotMaterializer` 位于当前包内，是类型，用于承载 `Directory Snapshot Materializer` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DirectorySnapshotMaterializer` is a type in its package and carries the responsibility, state, or contract for `Directory Snapshot Materializer`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Atomically switches validated provider data into the tenant directory view.
 */
@Repository
public class DirectorySnapshotMaterializer {

    /**
     * 字段 `entityManager` 表示 `DirectorySnapshotMaterializer` 中与 `entity Manager` 相关的状态、依赖、配置或结果（声明类型 `EntityManager`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `entityManager` stores the `entity Manager`-related state, dependency, configuration, or result of `DirectorySnapshotMaterializer` (declared type `EntityManager`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `entityManager` 时应保持 `DirectorySnapshotMaterializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `entityManager`, preserve `DirectorySnapshotMaterializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final EntityManager entityManager;
    /**
     * 字段 `idGenerator` 表示 `DirectorySnapshotMaterializer` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `DirectorySnapshotMaterializer` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `DirectorySnapshotMaterializer` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `DirectorySnapshotMaterializer`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;

    /**
     * 构造器 `DirectorySnapshotMaterializer` 用于创建并初始化 `DirectorySnapshotMaterializer` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DirectorySnapshotMaterializer` creates and initializes `DirectorySnapshotMaterializer`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DirectorySnapshotMaterializer` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DirectorySnapshotMaterializer`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param entityManager 输入参数 `entityManager`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DirectorySnapshotMaterializer(
            EntityManager entityManager,
            LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
    }

    /**
     * 方法 `apply` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `apply` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `apply` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `apply` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `apply` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `apply`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotVersion 输入参数 `snapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param model 输入参数 `model`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional
    public MaterializationResultVO apply(
            Long tenantId,
            Long snapshotId,
            long snapshotVersion,
            SnapshotModelVO model,
            String actorId,
            Instant now) {
        Map<String, Long> userIds = parseUserIds(model.userPositions());
        List<UserPositionSnapshotPO> existingUserPositions =
                lockActiveUserPositions(tenantId);
        Set<Long> requiredUserIds = new LinkedHashSet<>(userIds.values());
        existingUserPositions.stream()
                .map(UserPositionSnapshotPO::getUserId)
                .forEach(requiredUserIds::add);
        Map<Long, UserPO> users = lockUsers(tenantId, requiredUserIds);

        Counter counter = new Counter();
        Map<String, Long> unitIds = materializeUnits(
                tenantId, snapshotId, model.units(), actorId, now, counter);
        Map<String, Long> positionIds = materializePositions(
                tenantId, snapshotId, model.positions(), unitIds, actorId, now, counter);
        long affectedUsers = materializeUserPositions(
                tenantId, snapshotId, snapshotVersion, model.userPositions(),
                userIds, users, existingUserPositions, unitIds, positionIds,
                actorId, now, counter);
        return counter.result(affectedUsers);
    }

    /**
     * 方法 `materializeUnits` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `materialize Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `materializeUnits` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `materialize Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `materializeUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `materializeUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param counter 输入参数 `counter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Long> materializeUnits(
            Long tenantId,
            Long snapshotId,
            List<ResolvedUnitVO> units,
            String actorId,
            Instant now,
            Counter counter) {
        List<OrgUnitPO> existing = entityManager.createQuery("""
                        select o from OrgUnitEntity o where o.tenantId = :tenantId
                        """, OrgUnitPO.class)
                .setParameter("tenantId", tenantId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        Map<String, OrgUnitPO> byCode = new HashMap<>();
        existing.forEach(unit -> byCode.put(unit.getCode(), unit));

        Map<String, Long> providerIds = new LinkedHashMap<>();
        Set<String> incomingCodes = new HashSet<>();
        for (ResolvedUnitVO input : units) {
            Long parentId = input.parentId() == null ? null : providerIds.get(input.parentId());
            if (input.parentId() != null && parentId == null) {
                throw new Rbac3RuleViolation("DIRECTORY_ORG_REFERENCE_MISSING");
            }
            OrgUnitUnitTypeEnum unitType = OrgUnitUnitTypeEnum.valueOf(input.type());
            OrgUnitPO current = byCode.get(input.code());
            if (current == null) {
                current = new OrgUnitPO(
                        idGenerator.nextLongId(), tenantId, snapshotId, unitType,
                        input.code(), input.name(), parentId, input.path(), input.depth(),
                        input.externalId(), input.validFrom(), input.validTo(), actorId, now);
                entityManager.persist(current);
                counter.created++;
            } else {
                boolean unchanged = sameUnit(current, input, unitType, parentId);
                current.applySnapshot(
                        snapshotId, unitType, input.name(), parentId, input.path(),
                        input.depth(), input.externalId(), input.validFrom(), input.validTo(),
                        actorId, now);
                if (unchanged) {
                    counter.unchanged++;
                } else {
                    counter.updated++;
                }
            }
            incomingCodes.add(input.code());
            providerIds.put(input.id(), current.getId());
        }
        existing.stream()
                .filter(unit -> !incomingCodes.contains(unit.getCode()))
                .filter(unit -> unit.inactivate(actorId, now))
                .forEach(ignored -> counter.inactivated++);
        return providerIds;
    }

    /**
     * 方法 `materializePositions` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `materialize Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `materializePositions` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `materialize Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `materializePositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `materializePositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positions 输入参数 `positions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param unitIds 输入参数 `unitIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param counter 输入参数 `counter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Long> materializePositions(
            Long tenantId,
            Long snapshotId,
            List<PositionInputDTO> positions,
            Map<String, Long> unitIds,
            String actorId,
            Instant now,
            Counter counter) {
        List<PositionPO> existing = entityManager.createQuery("""
                        select p from PositionEntity p where p.tenantId = :tenantId
                        """, PositionPO.class)
                .setParameter("tenantId", tenantId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        Map<String, PositionPO> byCode = new HashMap<>();
        existing.forEach(position -> byCode.put(position.getCode(), position));

        Map<String, Long> providerIds = new LinkedHashMap<>();
        Set<String> incomingCodes = new HashSet<>();
        for (PositionInputDTO input : positions) {
            Long orgUnitId = unitIds.get(input.orgUnitId());
            if (orgUnitId == null) {
                throw new Rbac3RuleViolation("DIRECTORY_POSITION_ORG_MISSING");
            }
            PositionPO current = byCode.get(input.code());
            if (current == null) {
                current = new PositionPO(
                        idGenerator.nextLongId(), tenantId, snapshotId, input.code(),
                        input.name(), orgUnitId, input.externalId(), input.validFrom(),
                        input.validTo(), actorId, now);
                entityManager.persist(current);
                counter.created++;
            } else {
                boolean unchanged = samePosition(current, input, orgUnitId);
                current.applySnapshot(
                        snapshotId, input.name(), orgUnitId, input.externalId(),
                        input.validFrom(), input.validTo(), actorId, now);
                if (unchanged) {
                    counter.unchanged++;
                } else {
                    counter.updated++;
                }
            }
            incomingCodes.add(input.code());
            providerIds.put(input.id(), current.getId());
        }
        existing.stream()
                .filter(position -> !incomingCodes.contains(position.getCode()))
                .filter(position -> position.inactivate(actorId, now))
                .forEach(ignored -> counter.inactivated++);
        return providerIds;
    }

    /**
     * 方法 `materializeUserPositions` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `materialize User Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `materializeUserPositions` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `materialize User Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `materializeUserPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `materializeUserPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotVersion 输入参数 `snapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param inputs 输入参数 `inputs`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userIds 输入参数 `userIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param users 输入参数 `users`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param existing 输入参数 `existing`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param unitIds 输入参数 `unitIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionIds 输入参数 `positionIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param counter 输入参数 `counter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long materializeUserPositions(
            Long tenantId,
            Long snapshotId,
            long snapshotVersion,
            List<UserPositionInputDTO> inputs,
            Map<String, Long> userIds,
            Map<Long, UserPO> users,
            List<UserPositionSnapshotPO> existing,
            Map<String, Long> unitIds,
            Map<String, Long> positionIds,
            String actorId,
            Instant now,
            Counter counter) {
        Map<Long, Set<AssignmentSignature>> previousByUser = signaturesByUser(existing);
        Set<AssignmentSignature> previous = flatten(previousByUser);
        Set<UserPositionKey> previousKeys = keys(previous);
        Map<Long, Set<AssignmentSignature>> nextByUser = new HashMap<>();
        for (UserPositionInputDTO input : inputs) {
            Long userId = userIds.get(input.userId());
            Long positionId = positionIds.get(input.positionId());
            Long orgUnitId = unitIds.get(input.orgUnitId());
            if (positionId == null || orgUnitId == null) {
                throw new Rbac3RuleViolation("DIRECTORY_USER_POSITION_MISSING");
            }
            AssignmentSignature signature = new AssignmentSignature(
                    userId, positionId, orgUnitId, input.primary(),
                    input.externalAssignmentId(), input.validFrom(), input.validTo());
            nextByUser.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(signature);
            if (previous.contains(signature)) {
                counter.unchanged++;
            } else if (previousKeys.contains(signature.key())) {
                counter.updated++;
            } else {
                counter.created++;
            }
            entityManager.persist(new UserPositionSnapshotPO(
                    idGenerator.nextLongId(), tenantId, snapshotId, userId, positionId,
                    orgUnitId, input.primary(), input.externalAssignmentId(),
                    input.validFrom(), input.validTo(), actorId, now));
        }

        Set<UserPositionKey> nextKeys = keys(flatten(nextByUser));
        for (UserPositionSnapshotPO current : existing) {
            if (!nextKeys.contains(new UserPositionKey(
                    current.getUserId(), current.getPositionId()))) {
                counter.inactivated++;
            }
            current.inactivate(actorId, now);
        }

        Set<Long> allUserIds = new LinkedHashSet<>(previousByUser.keySet());
        allUserIds.addAll(nextByUser.keySet());
        long affectedUsers = 0;
        for (Long userId : allUserIds) {
            UserPO user = users.get(userId);
            if (user == null) {
                throw new Rbac3RuleViolation("DIRECTORY_USER_REFERENCE_MISSING");
            }
            Set<AssignmentSignature> previousAssignments = previousByUser.getOrDefault(
                    userId, Set.of());
            Set<AssignmentSignature> nextAssignments = nextByUser.getOrDefault(
                    userId, Set.of());
            boolean changed = !previousAssignments.equals(nextAssignments);
            AssignmentSignature primary = nextAssignments.stream()
                    .filter(AssignmentSignature::primary)
                    .findFirst().orElse(null);
            user.applyDirectorySnapshot(
                    snapshotVersion,
                    primary == null ? null : primary.orgUnitId(),
                    primary == null ? null : primary.positionId(),
                    changed, actorId, now);
            if (changed) {
                affectedUsers++;
            }
        }
        return affectedUsers;
    }

    /**
     * 方法 `lockActiveUserPositions` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `lock Active User Positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lockActiveUserPositions` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `lock Active User Positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lockActiveUserPositions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lockActiveUserPositions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<UserPositionSnapshotPO> lockActiveUserPositions(Long tenantId) {
        return entityManager.createQuery("""
                        select up from UserPositionSnapshotEntity up
                         where up.tenantId = :tenantId and up.status = :status
                        """, UserPositionSnapshotPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("status", UserPositionSnapshotStatusEnum.ACTIVE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    /**
     * 方法 `parseUserIds` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `parse User Ids` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parseUserIds` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `parse User Ids` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parseUserIds` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parseUserIds`, then continue the business flow using its result, exception, or side effect.
     *
     * @param inputs 输入参数 `inputs`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Long> parseUserIds(
            List<UserPositionInputDTO> inputs) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (UserPositionInputDTO input : inputs) {
            try {
                result.put(input.userId(), Long.valueOf(input.userId()));
            } catch (NumberFormatException exception) {
                throw new Rbac3RuleViolation("DIRECTORY_USER_ID_INVALID");
            }
        }
        return result;
    }

    /**
     * 方法 `lockUsers` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `lock Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `lockUsers` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `lock Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `lockUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `lockUsers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestedIds 输入参数 `requestedIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<Long, UserPO> lockUsers(Long tenantId, Iterable<Long> requestedIds) {
        List<Long> ids = new ArrayList<>();
        requestedIds.forEach(ids::add);
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UserPO> users = entityManager.createQuery("""
                        select u from UserEntity u
                         where u.tenantId = :tenantId and u.id in :ids
                        """, UserPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (users.size() != new HashSet<>(ids).size()) {
            throw new Rbac3RuleViolation("DIRECTORY_USER_REFERENCE_MISSING");
        }
        Map<Long, UserPO> result = new HashMap<>();
        users.forEach(user -> result.put(user.getId(), user));
        return result;
    }

    /**
     * 方法 `signaturesByUser` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `signatures By User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `signaturesByUser` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `signatures By User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `signaturesByUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `signaturesByUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param assignments 输入参数 `assignments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<Long, Set<AssignmentSignature>> signaturesByUser(
            List<UserPositionSnapshotPO> assignments) {
        Map<Long, Set<AssignmentSignature>> result = new HashMap<>();
        for (UserPositionSnapshotPO assignment : assignments) {
            result.computeIfAbsent(assignment.getUserId(), ignored -> new LinkedHashSet<>())
                    .add(new AssignmentSignature(
                            assignment.getUserId(), assignment.getPositionId(),
                            assignment.getOrgUnitId(), assignment.isPrimary(),
                            assignment.getExternalAssignmentId(), assignment.getValidFrom(),
                            assignment.getValidTo()));
        }
        return result;
    }

    /**
     * 方法 `flatten` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `flatten` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `flatten` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `flatten` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `flatten` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `flatten`, then continue the business flow using its result, exception, or side effect.
     *
     * @param values 输入参数 `values`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Set<AssignmentSignature> flatten(
            Map<Long, Set<AssignmentSignature>> values) {
        Set<AssignmentSignature> result = new LinkedHashSet<>();
        values.values().forEach(result::addAll);
        return result;
    }

    /**
     * 方法 `keys` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `keys` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `keys` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `keys` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `keys` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `keys`, then continue the business flow using its result, exception, or side effect.
     *
     * @param assignments 输入参数 `assignments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Set<UserPositionKey> keys(Set<AssignmentSignature> assignments) {
        Set<UserPositionKey> result = new HashSet<>();
        assignments.forEach(assignment -> result.add(assignment.key()));
        return result;
    }

    /**
     * 方法 `sameUnit` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `same Unit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sameUnit` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `same Unit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sameUnit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sameUnit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param current 输入参数 `current`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param input 输入参数 `input`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param unitType 输入参数 `unitType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean sameUnit(
            OrgUnitPO current,
            ResolvedUnitVO input,
            OrgUnitUnitTypeEnum unitType,
            Long parentId) {
        return current.getStatus() == OrgUnitStatusEnum.ACTIVE
                && current.getUnitType() == unitType
                && current.getName().equals(input.name())
                && Objects.equals(current.getParentId(), parentId)
                && current.getPath().equals(input.path())
                && current.getDepth() == input.depth()
                && Objects.equals(current.getExternalId(), input.externalId())
                && current.getValidFrom().equals(input.validFrom())
                && Objects.equals(current.getValidTo(), input.validTo());
    }

    /**
     * 方法 `samePosition` 按照 `DirectorySnapshotMaterializer` 的职责处理输入，完成 `same Position` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `samePosition` processes its inputs according to `DirectorySnapshotMaterializer`'s responsibility, performs the `same Position` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `samePosition` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `samePosition`, then continue the business flow using its result, exception, or side effect.
     *
     * @param current 输入参数 `current`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param input 输入参数 `input`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean samePosition(
            PositionPO current,
            PositionInputDTO input,
            Long orgUnitId) {
        return current.getStatus() == PositionStatusEnum.ACTIVE
                && current.getName().equals(input.name())
                && current.getOrgUnitId().equals(orgUnitId)
                && Objects.equals(current.getExternalId(), input.externalId())
                && current.getValidFrom().equals(input.validFrom())
                && Objects.equals(current.getValidTo(), input.validTo());
    }




    }
