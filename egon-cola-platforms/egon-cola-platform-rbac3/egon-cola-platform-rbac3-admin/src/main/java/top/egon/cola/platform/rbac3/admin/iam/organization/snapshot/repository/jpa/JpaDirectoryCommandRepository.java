package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.po.DirectorySnapshotPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.IngestionResultVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.MaterializationResultVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.SnapshotModelVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryCommandRepository;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.jpa.DirectorySnapshotMaterializer;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.jpa.JpaDirectorySnapshotRepository;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.enums.DirectorySnapshotOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.enums.DirectorySnapshotStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 目录写模型仓储；不拥有租户 catalog 或身份会员关系。 */
@Repository
public class JpaDirectoryCommandRepository implements DirectoryCommandRepository {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;
    private final JpaDirectorySnapshotRepository directorySnapshotStore;
    private final DirectorySnapshotMaterializer directorySnapshotMaterializer;
    private final DirectorySnapshotProcessor directorySnapshotProcessor =
            new DirectorySnapshotProcessor();

    public JpaDirectoryCommandRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock,
            JpaDirectorySnapshotRepository directorySnapshotStore,
            DirectorySnapshotMaterializer directorySnapshotMaterializer) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
        this.directorySnapshotStore = directorySnapshotStore;
        this.directorySnapshotMaterializer = directorySnapshotMaterializer;
    }

    @Override
    @Transactional
    public DirectorySyncVO submit(
            String tenantId,
            DirectorySnapshotCommandDTO command) {
        Instant now = databaseClock.transactionNow();
        SnapshotModelVO model = directorySnapshotProcessor.validate(
                command.payload(), command.generatedAt());
        DirectorySnapshotPO entity = new DirectorySnapshotPO(
                idGenerator.nextLongId(), Long.valueOf(tenantId), command.providerCode(),
                command.snapshotVersion(), command.checksum(), command.generatedAt(),
                command.payload(), "directory-sync", now);
        IngestionResultVO result = directorySnapshotStore.accept(entity);
        Map<String, Object> counts;
        long affectedUsers;
        if (result.outcome() == DirectorySnapshotOutcomeEnum.ACCEPTED) {
            MaterializationResultVO materialization = directorySnapshotMaterializer.apply(
                    Long.valueOf(tenantId), entity.getId(), command.snapshotVersion(),
                    model, "directory-sync", now);
            counts = new LinkedHashMap<>(model.counts());
            counts.putAll(materialization.counts());
            entity.validate(counts, "directory-sync", now);
            archiveCurrentSnapshot(
                    Long.valueOf(tenantId), command.providerCode(), entity.getId(), now);
            entity.activate("directory-sync", now);
            affectedUsers = materialization.affectedUserCount();
        } else {
            DirectorySnapshotPO existing = entityManager.find(
                    DirectorySnapshotPO.class, result.snapshotId());
            counts = existing == null ? Map.of() : existing.getCounts();
            affectedUsers = numericCount(counts, "affectedUsers");
        }
        return new DirectorySyncVO(
                result.snapshotId().toString(), result.outcome().name(),
                longCounts(counts), affectedUsers);
    }

    @Override
    @Transactional
    public UserDirectoryVO changeUserStatus(
            String tenantId,
            String userId,
            UserStatusCommandDTO command,
            String actorId) {
        UserPO user = entityManager.find(
                UserPO.class, Long.valueOf(userId), LockModeType.PESSIMISTIC_WRITE);
        if (user == null || !Long.valueOf(tenantId).equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        UserStatusEnum nextStatus = enumValue(
                UserStatusEnum.class, command.status(), "USER_STATUS_INVALID");
        Instant now = databaseClock.transactionNow();
        user.changeStatus(nextStatus, command.reason(), command.expectedAuthVersion(),
                actorId, now);
        return userView(user);
    }

    private UserDirectoryVO userView(UserPO user) {
        return new UserDirectoryVO(
                user.getId().toString(), user.getIdentitySub(),
                user.getStatus().name(), user.getAuthVersion());
    }

    private void archiveCurrentSnapshot(
            Long tenantId,
            String providerCode,
            Long incomingSnapshotId,
            Instant now) {
        List<DirectorySnapshotPO> active = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.status = :status
                           and s.id <> :incomingSnapshotId
                        """, DirectorySnapshotPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("providerCode", providerCode)
                .setParameter("status", DirectorySnapshotStatusEnum.ACTIVE)
                .setParameter("incomingSnapshotId", incomingSnapshotId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        active.forEach(snapshot -> snapshot.archive("directory-sync", now));
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String reasonCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new Rbac3RuleViolation(reasonCode);
        }
    }

    private Map<String, Long> longCounts(Map<String, Object> values) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Number number) {
                result.put(key, number.longValue());
            }
        });
        return Map.copyOf(result);
    }

    private long numericCount(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
}
