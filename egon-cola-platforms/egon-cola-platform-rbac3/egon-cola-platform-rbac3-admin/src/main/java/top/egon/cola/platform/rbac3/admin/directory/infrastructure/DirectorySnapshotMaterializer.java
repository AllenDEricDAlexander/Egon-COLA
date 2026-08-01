package top.egon.cola.platform.rbac3.admin.directory.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.directory.application.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.domain.OrgUnitEntity;
import top.egon.cola.platform.rbac3.admin.directory.domain.PositionEntity;
import top.egon.cola.platform.rbac3.admin.directory.domain.UserPositionSnapshotEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
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

/** Atomically switches validated provider data into the tenant directory view. */
@Repository
public class DirectorySnapshotMaterializer {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;

    public DirectorySnapshotMaterializer(
            EntityManager entityManager,
            LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public MaterializationResult apply(
            Long tenantId,
            Long snapshotId,
            long snapshotVersion,
            DirectorySnapshotProcessor.SnapshotModel model,
            String actorId,
            Instant now) {
        Map<String, Long> userIds = parseUserIds(model.userPositions());
        List<UserPositionSnapshotEntity> existingUserPositions =
                lockActiveUserPositions(tenantId);
        Set<Long> requiredUserIds = new LinkedHashSet<>(userIds.values());
        existingUserPositions.stream()
                .map(UserPositionSnapshotEntity::getUserId)
                .forEach(requiredUserIds::add);
        Map<Long, UserEntity> users = lockUsers(tenantId, requiredUserIds);

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

    private Map<String, Long> materializeUnits(
            Long tenantId,
            Long snapshotId,
            List<DirectorySnapshotProcessor.ResolvedUnit> units,
            String actorId,
            Instant now,
            Counter counter) {
        List<OrgUnitEntity> existing = entityManager.createQuery("""
                        select o from OrgUnitEntity o where o.tenantId = :tenantId
                        """, OrgUnitEntity.class)
                .setParameter("tenantId", tenantId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        Map<String, OrgUnitEntity> byCode = new HashMap<>();
        existing.forEach(unit -> byCode.put(unit.getCode(), unit));

        Map<String, Long> providerIds = new LinkedHashMap<>();
        Set<String> incomingCodes = new HashSet<>();
        for (DirectorySnapshotProcessor.ResolvedUnit input : units) {
            Long parentId = input.parentId() == null ? null : providerIds.get(input.parentId());
            if (input.parentId() != null && parentId == null) {
                throw new Rbac3RuleViolation("DIRECTORY_ORG_REFERENCE_MISSING");
            }
            OrgUnitEntity.UnitType unitType = OrgUnitEntity.UnitType.valueOf(input.type());
            OrgUnitEntity current = byCode.get(input.code());
            if (current == null) {
                current = new OrgUnitEntity(
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

    private Map<String, Long> materializePositions(
            Long tenantId,
            Long snapshotId,
            List<DirectorySnapshotProcessor.PositionInput> positions,
            Map<String, Long> unitIds,
            String actorId,
            Instant now,
            Counter counter) {
        List<PositionEntity> existing = entityManager.createQuery("""
                        select p from PositionEntity p where p.tenantId = :tenantId
                        """, PositionEntity.class)
                .setParameter("tenantId", tenantId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        Map<String, PositionEntity> byCode = new HashMap<>();
        existing.forEach(position -> byCode.put(position.getCode(), position));

        Map<String, Long> providerIds = new LinkedHashMap<>();
        Set<String> incomingCodes = new HashSet<>();
        for (DirectorySnapshotProcessor.PositionInput input : positions) {
            Long orgUnitId = unitIds.get(input.orgUnitId());
            if (orgUnitId == null) {
                throw new Rbac3RuleViolation("DIRECTORY_POSITION_ORG_MISSING");
            }
            PositionEntity current = byCode.get(input.code());
            if (current == null) {
                current = new PositionEntity(
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

    private long materializeUserPositions(
            Long tenantId,
            Long snapshotId,
            long snapshotVersion,
            List<DirectorySnapshotProcessor.UserPositionInput> inputs,
            Map<String, Long> userIds,
            Map<Long, UserEntity> users,
            List<UserPositionSnapshotEntity> existing,
            Map<String, Long> unitIds,
            Map<String, Long> positionIds,
            String actorId,
            Instant now,
            Counter counter) {
        Map<Long, Set<AssignmentSignature>> previousByUser = signaturesByUser(existing);
        Set<AssignmentSignature> previous = flatten(previousByUser);
        Set<UserPositionKey> previousKeys = keys(previous);
        Map<Long, Set<AssignmentSignature>> nextByUser = new HashMap<>();
        for (DirectorySnapshotProcessor.UserPositionInput input : inputs) {
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
            entityManager.persist(new UserPositionSnapshotEntity(
                    idGenerator.nextLongId(), tenantId, snapshotId, userId, positionId,
                    orgUnitId, input.primary(), input.externalAssignmentId(),
                    input.validFrom(), input.validTo(), actorId, now));
        }

        Set<UserPositionKey> nextKeys = keys(flatten(nextByUser));
        for (UserPositionSnapshotEntity current : existing) {
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
            UserEntity user = users.get(userId);
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

    private List<UserPositionSnapshotEntity> lockActiveUserPositions(Long tenantId) {
        return entityManager.createQuery("""
                        select up from UserPositionSnapshotEntity up
                         where up.tenantId = :tenantId and up.status = :status
                        """, UserPositionSnapshotEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("status", UserPositionSnapshotEntity.Status.ACTIVE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    private Map<String, Long> parseUserIds(
            List<DirectorySnapshotProcessor.UserPositionInput> inputs) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (DirectorySnapshotProcessor.UserPositionInput input : inputs) {
            try {
                result.put(input.userId(), Long.valueOf(input.userId()));
            } catch (NumberFormatException exception) {
                throw new Rbac3RuleViolation("DIRECTORY_USER_ID_INVALID");
            }
        }
        return result;
    }

    private Map<Long, UserEntity> lockUsers(Long tenantId, Iterable<Long> requestedIds) {
        List<Long> ids = new ArrayList<>();
        requestedIds.forEach(ids::add);
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = entityManager.createQuery("""
                        select u from UserEntity u
                         where u.tenantId = :tenantId and u.id in :ids
                        """, UserEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (users.size() != new HashSet<>(ids).size()) {
            throw new Rbac3RuleViolation("DIRECTORY_USER_REFERENCE_MISSING");
        }
        Map<Long, UserEntity> result = new HashMap<>();
        users.forEach(user -> result.put(user.getId(), user));
        return result;
    }

    private Map<Long, Set<AssignmentSignature>> signaturesByUser(
            List<UserPositionSnapshotEntity> assignments) {
        Map<Long, Set<AssignmentSignature>> result = new HashMap<>();
        for (UserPositionSnapshotEntity assignment : assignments) {
            result.computeIfAbsent(assignment.getUserId(), ignored -> new LinkedHashSet<>())
                    .add(new AssignmentSignature(
                            assignment.getUserId(), assignment.getPositionId(),
                            assignment.getOrgUnitId(), assignment.isPrimary(),
                            assignment.getExternalAssignmentId(), assignment.getValidFrom(),
                            assignment.getValidTo()));
        }
        return result;
    }

    private Set<AssignmentSignature> flatten(
            Map<Long, Set<AssignmentSignature>> values) {
        Set<AssignmentSignature> result = new LinkedHashSet<>();
        values.values().forEach(result::addAll);
        return result;
    }

    private Set<UserPositionKey> keys(Set<AssignmentSignature> assignments) {
        Set<UserPositionKey> result = new HashSet<>();
        assignments.forEach(assignment -> result.add(assignment.key()));
        return result;
    }

    private boolean sameUnit(
            OrgUnitEntity current,
            DirectorySnapshotProcessor.ResolvedUnit input,
            OrgUnitEntity.UnitType unitType,
            Long parentId) {
        return current.getStatus() == OrgUnitEntity.Status.ACTIVE
                && current.getUnitType() == unitType
                && current.getName().equals(input.name())
                && Objects.equals(current.getParentId(), parentId)
                && current.getPath().equals(input.path())
                && current.getDepth() == input.depth()
                && Objects.equals(current.getExternalId(), input.externalId())
                && current.getValidFrom().equals(input.validFrom())
                && Objects.equals(current.getValidTo(), input.validTo());
    }

    private boolean samePosition(
            PositionEntity current,
            DirectorySnapshotProcessor.PositionInput input,
            Long orgUnitId) {
        return current.getStatus() == PositionEntity.Status.ACTIVE
                && current.getName().equals(input.name())
                && current.getOrgUnitId().equals(orgUnitId)
                && Objects.equals(current.getExternalId(), input.externalId())
                && current.getValidFrom().equals(input.validFrom())
                && Objects.equals(current.getValidTo(), input.validTo());
    }

    public record MaterializationResult(
            long created,
            long updated,
            long inactivated,
            long unchanged,
            long conflict,
            long affectedUserCount) {

        public Map<String, Object> counts() {
            return Map.of(
                    "created", created,
                    "updated", updated,
                    "inactivated", inactivated,
                    "unchanged", unchanged,
                    "conflict", conflict,
                    "affectedUsers", affectedUserCount);
        }
    }

    private static final class Counter {
        private long created;
        private long updated;
        private long inactivated;
        private long unchanged;

        private MaterializationResult result(long affectedUsers) {
            return new MaterializationResult(
                    created, updated, inactivated, unchanged, 0, affectedUsers);
        }
    }

    private record UserPositionKey(Long userId, Long positionId) {
    }

    private record AssignmentSignature(
            Long userId,
            Long positionId,
            Long orgUnitId,
            boolean primary,
            String externalAssignmentId,
            Instant validFrom,
            Instant validTo) {

        private UserPositionKey key() {
            return new UserPositionKey(userId, positionId);
        }
    }
}
