package top.egon.cola.platform.rbac3.admin.iam.organization.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.DirectorySourceTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.OrgUnitUnitTypeEnum;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Owns CRUD and tree operations for MANUAL organization records. */
@Service
public final class OrganizationFacade {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public OrganizationFacade(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
    }

    @Transactional(readOnly = true)
    public List<OrgUnitVO> list(Long tenantId, Long parentId) {
        List<OrgUnitPO> values = parentId == null
                ? entityManager.createQuery("""
                        select o from OrgUnitEntity o
                         where o.tenantId = :tenantId
                         order by o.path, o.id
                        """, OrgUnitPO.class)
                .setParameter("tenantId", tenantId)
                .getResultList()
                : entityManager.createQuery("""
                        select o from OrgUnitEntity o
                         where o.tenantId = :tenantId and o.parentId = :parentId
                         order by o.path, o.id
                        """, OrgUnitPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("parentId", parentId)
                .getResultList();
        return values.stream().map(OrganizationFacade::view).toList();
    }

    @Transactional
    public OrgUnitVO create(
            Long tenantId,
            CreateCommand command,
            String actorId) {
        Objects.requireNonNull(command, "command");
        Instant now = databaseClock.transactionNow();
        OrgUnitPO parent = parent(tenantId, command.parentId());
        String code = required(command.code(), "code");
        if (existsCode(tenantId, code)) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_CODE_CONFLICT");
        }
        String path = parent == null ? code : parent.getPath() + "/" + code;
        int depth = parent == null ? 0 : parent.getDepth() + 1;
        OrgUnitPO value = new OrgUnitPO(
                idGenerator.nextLongId(), tenantId, DirectorySourceTypeEnum.MANUAL,
                null, Objects.requireNonNull(command.type(), "type"), code,
                required(command.name(), "name"), command.parentId(), path, depth,
                command.externalId(), Objects.requireNonNull(command.validFrom(), "validFrom"),
                command.validTo(), actorId, now);
        entityManager.persist(value);
        return view(value);
    }

    @Transactional
    public OrgUnitVO update(
            Long tenantId,
            Long orgUnitId,
            UpdateCommand command,
            String actorId) {
        Objects.requireNonNull(command, "command");
        OrgUnitPO current = require(tenantId, orgUnitId);
        requireManual(current);
        OrgUnitPO parent = parent(tenantId, command.parentId());
        rejectSelfParent(orgUnitId, command.parentId());
        rejectCycle(tenantId, orgUnitId, command.parentId());
        String path = parent == null ? current.getCode() : parent.getPath() + "/" + current.getCode();
        int depth = parent == null ? 0 : parent.getDepth() + 1;
        rewriteSubtree(tenantId, current, command.parentId(), path, depth,
                command, actorId);
        return view(current);
    }

    @Transactional
    public void remove(Long tenantId, Long orgUnitId, long expectedVersion, String actorId) {
        OrgUnitPO current = require(tenantId, orgUnitId);
        requireManual(current);
        if (entityManager.createQuery("""
                        select count(o) from OrgUnitEntity o
                         where o.tenantId = :tenantId and o.parentId = :orgUnitId
                           and o.status = :status
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("orgUnitId", orgUnitId)
                .setParameter("status", OrgUnitStatusEnum.ACTIVE)
                .getSingleResult() > 0) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_HAS_CHILDREN");
        }
        if (current.getVersion() != expectedVersion) {
            throw new Rbac3RuleViolation("DIRECTORY_VERSION_CONFLICT");
        }
        current.inactivateManually(actorId, databaseClock.transactionNow());
    }

    private void rewriteSubtree(
            Long tenantId,
            OrgUnitPO current,
            Long newParentId,
            String nextPath,
            int nextDepth,
            UpdateCommand command,
            String actorId) {
        String oldPath = current.getPath();
        List<OrgUnitPO> descendants = entityManager.createQuery("""
                        select o from OrgUnitEntity o
                         where o.tenantId = :tenantId and o.path like :prefix
                         order by o.depth, o.id
                        """, OrgUnitPO.class)
                .setParameter("tenantId", tenantId)
                .setParameter("prefix", oldPath + "/%")
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (descendants.stream().anyMatch(value ->
                value.getSourceType() != DirectorySourceTypeEnum.MANUAL)) {
            throw new Rbac3RuleViolation("DIRECTORY_SNAPSHOT_DESCENDANT_READ_ONLY");
        }
        Instant now = databaseClock.transactionNow();
        int oldDepth = current.getDepth();
        current.updateManually(
                command.type(), command.name(), newParentId, nextPath, nextDepth,
                command.externalId(), command.validFrom(), command.validTo(),
                command.expectedVersion(), actorId, now);
        for (OrgUnitPO descendant : descendants) {
            String suffix = descendant.getPath().substring(oldPath.length());
            descendant.updateManually(
                    descendant.getUnitType(), descendant.getName(), descendant.getParentId(),
                    nextPath + suffix, nextDepth + descendant.getDepth() - oldDepth,
                    descendant.getExternalId(), descendant.getValidFrom(), descendant.getValidTo(),
                    descendant.getVersion(), actorId, now);
        }
    }

    private OrgUnitPO require(Long tenantId, Long orgUnitId) {
        OrgUnitPO value = entityManager.find(OrgUnitPO.class, orgUnitId,
                LockModeType.PESSIMISTIC_WRITE);
        if (value == null || !tenantId.equals(value.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return value;
    }

    private OrgUnitPO parent(Long tenantId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        OrgUnitPO value = require(tenantId, parentId);
        if (value.getStatus() != OrgUnitStatusEnum.ACTIVE) {
            throw new Rbac3RuleViolation("DIRECTORY_PARENT_INACTIVE");
        }
        return value;
    }

    private boolean existsCode(Long tenantId, String code) {
        return entityManager.createQuery("""
                        select count(o) from OrgUnitEntity o
                         where o.tenantId = :tenantId and o.code = :code
                           and o.status = :status
                        """, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("code", code)
                .setParameter("status", OrgUnitStatusEnum.ACTIVE)
                .getSingleResult() > 0;
    }

    private void rejectSelfParent(Long orgUnitId, Long parentId) {
        if (Objects.equals(orgUnitId, parentId)) {
            throw new Rbac3RuleViolation("DIRECTORY_ORG_CYCLE");
        }
    }

    private void rejectCycle(Long tenantId, Long orgUnitId, Long parentId) {
        Long current = parentId;
        while (current != null) {
            if (current.equals(orgUnitId)) {
                throw new Rbac3RuleViolation("DIRECTORY_ORG_CYCLE");
            }
            OrgUnitPO value = entityManager.find(OrgUnitPO.class, current);
            if (value == null || !tenantId.equals(value.getTenantId())) {
                throw new Rbac3RuleViolation("DIRECTORY_PARENT_NOT_FOUND");
            }
            current = value.getParentId();
        }
    }

    private static OrgUnitVO view(OrgUnitPO value) {
        return new OrgUnitVO(
                value.getId().toString(),
                value.getSnapshotId() == null ? null : value.getSnapshotId().toString(),
                value.getUnitType().name(), value.getCode(), value.getName(),
                value.getParentId() == null ? null : value.getParentId().toString(),
                value.getPath(), value.getDepth(), value.getStatus().name());
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void requireManual(OrgUnitPO value) {
        if (value.getSourceType() != DirectorySourceTypeEnum.MANUAL) {
            throw new IllegalStateException("directory snapshot organization is read-only");
        }
    }

    public record CreateCommand(
            OrgUnitUnitTypeEnum type,
            String code,
            String name,
            Long parentId,
            String externalId,
            Instant validFrom,
            Instant validTo) {
    }

    public record UpdateCommand(
            OrgUnitUnitTypeEnum type,
            String name,
            Long parentId,
            String externalId,
            Instant validFrom,
            Instant validTo,
            long expectedVersion) {
    }

}
