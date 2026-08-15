package top.egon.cola.platform.rbac3.admin.iam.position.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.DirectorySourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.enums.PositionStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Owns CRUD for MANUAL positions and keeps them bound to one organization. */
@Service
public final class PositionFacade {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public PositionFacade(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
    }

    @Transactional(readOnly = true)
    public List<PositionVO> list(Long tenantId, Long orgUnitId) {
        List<PositionPO> values = orgUnitId == null
                ? entityManager.createQuery("""
                        select p from PositionEntity p
                         where p.tenantId = :tenantId
                         order by p.code, p.id
                        """, PositionPO.class)
                .setParameter("tenantId", tenantId)
                .getResultList()
                : entityManager.createQuery("""
                        select p from PositionEntity p
                         where p.tenantId = :tenantId and p.orgUnitId = :orgUnitId
                         order by p.code, p.id
                        """, PositionPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("orgUnitId", orgUnitId)
                .getResultList();
        return values.stream().map(PositionFacade::view).toList();
    }

    @Transactional
    public PositionVO create(
            Long tenantId,
            CreateCommand command,
            String actorId) {
        Objects.requireNonNull(command, "command");
        OrgUnitPO organization = activeOrganization(tenantId, command.orgUnitId());
        String code = required(command.code(), "code");
        if (entityManager.createQuery("""
                        select count(p) from PositionEntity p
                         where p.tenantId = :tenantId and p.code = :code
                           and p.status = :status
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("code", code)
                .setParameter("status", PositionStatusEnum.ACTIVE)
                .getSingleResult() > 0) {
            throw new Rbac3RuleViolation("DIRECTORY_POSITION_CODE_CONFLICT");
        }
        Instant now = databaseClock.transactionNow();
        PositionPO value = new PositionPO(
                idGenerator.nextLongId(), tenantId, DirectorySourceTypeEnum.MANUAL,
                null, code, required(command.name(), "name"), organization.getId(),
                command.externalId(), Objects.requireNonNull(command.validFrom(), "validFrom"),
                command.validTo(), actorId, now);
        entityManager.persist(value);
        return view(value);
    }

    @Transactional
    public PositionVO update(
            Long tenantId,
            Long positionId,
            UpdateCommand command,
            String actorId) {
        Objects.requireNonNull(command, "command");
        PositionPO value = require(tenantId, positionId);
        requireManual(value);
        OrgUnitPO organization = activeOrganization(tenantId, command.orgUnitId());
        value.updateManually(
                command.name(), organization.getId(), command.externalId(),
                command.validFrom(), command.validTo(), command.expectedVersion(),
                actorId, databaseClock.transactionNow());
        return view(value);
    }

    @Transactional
    public void remove(Long tenantId, Long positionId, long expectedVersion, String actorId) {
        PositionPO value = require(tenantId, positionId);
        requireManual(value);
        if (value.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("DIRECTORY_VERSION_CONFLICT");
        }
        value.inactivateManually(actorId, databaseClock.transactionNow());
    }

    private PositionPO require(Long tenantId, Long positionId) {
        PositionPO value = entityManager.find(PositionPO.class, positionId,
                LockModeType.PESSIMISTIC_WRITE);
        if (value == null || !tenantId.equals(value.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return value;
    }

    private OrgUnitPO activeOrganization(Long tenantId, Long orgUnitId) {
        OrgUnitPO value = entityManager.find(OrgUnitPO.class, orgUnitId);
        if (value == null || !tenantId.equals(value.getTenantId())) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_NOT_FOUND");
        }
        if (value.getStatus() != OrgUnitStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_INACTIVE");
        }
        return value;
    }

    private static void requireManual(PositionPO value) {
        if (value.getSourceType() != DirectorySourceTypeEnum.MANUAL) {
            throw new IllegalStateException("directory snapshot position is read-only");
        }
    }

    private static PositionVO view(PositionPO value) {
        return new PositionVO(
                value.getId().toString(),
                value.getSnapshotId() == null ? null : value.getSnapshotId().toString(),
                value.getCode(), value.getName(), value.getOrgUnitId().toString(),
                value.getStatus().name());
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public record CreateCommand(
            String code,
            String name,
            Long orgUnitId,
            String externalId,
            Instant validFrom,
            Instant validTo) {
    }

    public record UpdateCommand(
            String name,
            Long orgUnitId,
            String externalId,
            Instant validFrom,
            Instant validTo,
            long expectedVersion) {
    }
}
