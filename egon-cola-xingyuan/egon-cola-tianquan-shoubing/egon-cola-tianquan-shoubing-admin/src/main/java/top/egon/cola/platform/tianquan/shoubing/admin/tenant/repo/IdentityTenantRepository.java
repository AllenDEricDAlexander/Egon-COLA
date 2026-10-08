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
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.dao.IdentityTenantDAO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter.IdentityTenantConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO;

import java.util.List;
import java.util.Optional;

/** Persistence access for Tianquan-Shoubing tenant catalog facts. */
@Repository("identityTenantRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class IdentityTenantRepository extends EgonColaRepository<IdentityTenantDAO, IdentityTenantPO> {

    @Getter
    @Qualifier("identityTenantDAO")
    private final IdentityTenantDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("identityTenantConverterImpl")
    private final IdentityTenantConverter converter;
    private final IdentityDirectoryContext directoryContext;

    @Transactional(readOnly = true)
    public boolean existsByTenantCodeIgnoreCase(@NotBlank @Size(max = 64) String tenantCode) {
        String code = required(tenantCode, "tenantCode").toLowerCase(java.util.Locale.ROOT);
        return directoryContext.withPlatformDirectoryPartition(() ->
                baseMapper.countByTenantCode(directoryContext.directoryPartitionId(), code) > 0L);
    }

    @Transactional(readOnly = true)
    public Optional<IdentityTenantBO> findByTenantCodeIgnoreCase(
            @NotBlank @Size(max = 64) String tenantCode) {
        String code = required(tenantCode, "tenantCode").toLowerCase(java.util.Locale.ROOT);
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByTenantCode(
                                directoryContext.directoryPartitionId(), code))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public Optional<IdentityTenantBO> findById(@NotBlank @Size(max = 64) String identityTenantId) {
        String id = required(identityTenantId, "identityTenantId");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByExternalKey(
                                directoryContext.directoryPartitionId(), id))
                        .map(converter::toTarget));
    }

    @Transactional
    public Optional<IdentityTenantBO> findByIdForUpdate(
            @NotBlank @Size(max = 64) String identityTenantId) {
        String id = required(identityTenantId, "identityTenantId");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectForUpdate(
                                directoryContext.directoryPartitionId(), id))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public List<IdentityTenantBO> findAllByOrderByUpdatedAtDescIdAsc() {
        return directoryContext.withPlatformDirectoryPartition(() -> baseMapper
                .selectDirectory(directoryContext.directoryPartitionId()).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Transactional
    public IdentityTenantBO save(@NotNull IdentityTenantBO tenant) {
        String id = required(tenant.getIdentityTenantId(), "identityTenantId");
        String actor = required(tenant.getUpdatedBy(), "updatedBy");
        return directoryContext.withPlatformDirectoryPartition(actor, () -> {
            Long partitionId = directoryContext.directoryPartitionId();
            IdentityTenantPO current = baseMapper.selectForUpdate(partitionId, id);
            IdentityTenantPO value = converter.toSource(tenant);
            value.setTenantId(partitionId);
            if (current == null) {
                if (tenant.getVersion() != 0L) {
                    throw stale(id, tenant.getVersion() - 1L, -1L);
                }
                if (!super.save(value)) {
                    throw new IllegalStateException("IDENTITY_TENANT_INSERT_NOT_APPLIED");
                }
                return converter.toTarget(value);
            }

            long expectedVersion = tenant.getVersion() - 1L;
            if (tenant.getVersion() <= 0L || current.getVersion() != expectedVersion) {
                throw stale(id, expectedVersion, current.getVersion());
            }
            value.setId(current.getId());
            value.setVersion(tenant.getVersion());
            if (baseMapper.updateExpectedVersion(partitionId, value, expectedVersion) != 1) {
                throw stale(id, expectedVersion, current.getVersion());
            }
            return converter.toTarget(value);
        });
    }

    private static OptimisticLockingFailureException stale(
            String identityTenantId,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException(
                "tenant " + identityTenantId + " expected version "
                        + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
