package top.egon.cola.platform.tianquan.shoubing.admin.tenant.repo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.dao.IdentityTenantMembershipDAO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter.IdentityTenantMembershipConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantMembershipPO;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** Persistence access for Tianquan-Shoubing tenant membership facts. */
@Repository("identityTenantMembershipRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class IdentityTenantMembershipRepository
        extends EgonColaRepository<IdentityTenantMembershipDAO, IdentityTenantMembershipPO> {

    @Getter
    @Qualifier("identityTenantMembershipDAO")
    private final IdentityTenantMembershipDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("identityTenantMembershipConverterImpl")
    private final IdentityTenantMembershipConverter converter;
    private final IdentityDirectoryContext directoryContext;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<IdentityTenantMembershipBO> findByMembershipId(
            @NotBlank @Size(max = 64) String membershipId) {
        String value = required(membershipId, "membershipId");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByExternalKey(
                                directoryContext.directoryPartitionId(), value))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public Optional<IdentityTenantMembershipBO> findByTenantIdAndIdentitySub(
            @NotBlank @Size(max = 64) String identityTenantId,
            @NotBlank @Size(max = 64) String identitySub) {
        String tenant = required(identityTenantId, "tenantId");
        String subject = required(identitySub, "identitySub");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByTenantAndIdentitySub(
                                directoryContext.directoryPartitionId(), tenant, subject))
                        .map(converter::toTarget));
    }

    @Transactional
    public Optional<IdentityTenantMembershipBO> findByTenantIdAndIdentitySubForUpdate(
            @NotBlank @Size(max = 64) String identityTenantId,
            @NotBlank @Size(max = 64) String identitySub) {
        String tenant = required(identityTenantId, "tenantId");
        String subject = required(identitySub, "identitySub");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectForUpdate(
                                directoryContext.directoryPartitionId(), tenant, subject))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public List<IdentityTenantMembershipBO> findByTenantIdOrderByUpdatedAtDescIdentitySubAsc(
            @NotBlank @Size(max = 64) String identityTenantId) {
        String tenant = required(identityTenantId, "tenantId");
        return directoryContext.withPlatformDirectoryPartition(() -> converter.toTargetList(
                baseMapper.selectByTenant(directoryContext.directoryPartitionId(), tenant)));
    }

    @Transactional(readOnly = true)
    public List<IdentityTenantMembershipBO> findByIdentitySubOrderByUpdatedAtDescTenantIdAsc(
            @NotBlank @Size(max = 64) String identitySub) {
        String subject = required(identitySub, "identitySub");
        return directoryContext.withPlatformDirectoryPartition(() -> converter.toTargetList(
                baseMapper.selectByIdentitySub(directoryContext.directoryPartitionId(), subject)));
    }

    @Transactional
    public IdentityTenantMembershipBO save(@NotNull IdentityTenantMembershipBO membership) {
        String membershipId = required(membership.getMembershipId(), "membershipId");
        String tenantId = required(membership.getIdentityTenantId(), "identityTenantId");
        String identitySub = required(membership.getIdentitySub(), "identitySub");
        String actor = required(membership.getUpdatedBy(), "updatedBy");
        if (tenantId.equals(Long.toString(directoryContext.directoryPartitionId()))) {
            throw new IllegalArgumentException("PLATFORM_PARTITION_CANNOT_BE_BUSINESS_TENANT");
        }
        return directoryContext.withPlatformDirectoryPartition(actor, () -> {
            Long partitionId = directoryContext.directoryPartitionId();
            IdentityTenantMembershipPO current = baseMapper.selectForUpdate(
                    partitionId, tenantId, identitySub);
            IdentityTenantMembershipPO value = converter.toSource(membership);
            value.setTenantId(partitionId);
            if (current == null) {
                if (membership.getVersion() != 0L) {
                    throw stale(membershipId, membership.getVersion() - 1L, -1L);
                }
                if (!super.save(value)) {
                    throw new IllegalStateException("IDENTITY_MEMBERSHIP_INSERT_NOT_APPLIED");
                }
                return converter.toTarget(value);
            }

            long expectedVersion = membership.getVersion() - 1L;
            if (membership.getVersion() <= 0L || current.getVersion() != expectedVersion
                    || !membershipId.equals(current.getMembershipId())) {
                throw stale(membershipId, expectedVersion, current.getVersion());
            }
            value.setId(current.getId());
            value.setVersion(membership.getVersion());
            value.setUpdateUserId(userIdProvider.currentUserId());
            value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            if (baseMapper.updateExpectedVersion(partitionId, value, expectedVersion) != 1) {
                throw stale(membershipId, expectedVersion, current.getVersion());
            }
            return converter.toTarget(value);
        });
    }

    private static OptimisticLockingFailureException stale(
            String membershipId,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException(
                "membership " + membershipId + " expected version "
                        + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
