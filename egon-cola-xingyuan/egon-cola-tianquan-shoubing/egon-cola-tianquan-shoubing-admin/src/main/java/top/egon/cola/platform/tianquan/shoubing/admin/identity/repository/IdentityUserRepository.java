package top.egon.cola.platform.tianquan.shoubing.admin.identity.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.cola.platform.tianquan.shoubing.admin.identity.dao.IdentityUserDAO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter.IdentityUserConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository("identityUserRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class IdentityUserRepository extends EgonColaRepository<IdentityUserDAO, IdentityUserPO> {

    @Getter
    @Qualifier("identityUserDAO")
    private final IdentityUserDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("identityUserConverterImpl")
    private final IdentityUserConverter converter;
    private final IdentityDirectoryContext directoryContext;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findByNormalizedUsername(
            @NotBlank @Size(max = 128) String normalizedUsername) {
        String value = required(normalizedUsername, "normalizedUsername");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByNormalizedUsername(
                                directoryContext.directoryPartitionId(), value))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public Optional<IdentityUser> findByIdentitySub(@NotBlank @Size(max = 64) String identitySub) {
        String value = required(identitySub, "identitySub");
        return directoryContext.withPlatformDirectoryPartition(() ->
                Optional.ofNullable(baseMapper.selectByExternalKey(
                                directoryContext.directoryPartitionId(), value))
                        .map(converter::toTarget));
    }

    @Transactional(readOnly = true)
    public List<IdentityUser> listOrderedByNormalizedUsernameAndSubject() {
        return directoryContext.withPlatformDirectoryPartition(() -> baseMapper
                .selectDirectory(directoryContext.directoryPartitionId()).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Transactional
    public IdentityUser save(@NotNull IdentityUser user, @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(user, "user");
        return directoryContext.withPlatformDirectoryPartition(() -> {
            Long partitionId = directoryContext.directoryPartitionId();
            IdentityUserPO current = baseMapper.selectForUpdate(partitionId, user.id());
            if (current == null) {
                requireNewVersion(user.id(), user.version(), expectedVersion);
                IdentityUserPO inserted = converter.toSource(user);
                if (!super.save(inserted)) {
                    throw new IllegalStateException("IDENTITY_USER_INSERT_NOT_APPLIED");
                }
                return converter.toTarget(inserted);
            }

            requireExpectedVersion(user.id(), current.getVersion(), expectedVersion, user.version());
            IdentityUserPO update = converter.toSource(user);
            update.setId(current.getId());
            update.setTenantId(partitionId);
            update.setVersion(user.version());
            update.setUpdateUserId(userIdProvider.currentUserId());
            update.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            if (baseMapper.updateExpectedVersion(partitionId, update, expectedVersion) != 1) {
                throw stale(user.id(), expectedVersion, current.getVersion());
            }
            return converter.toTarget(update);
        });
    }

    private static void requireNewVersion(String identitySub, long newVersion, long expectedVersion) {
        if (expectedVersion != 0L || newVersion != 0L) {
            throw stale(identitySub, expectedVersion, -1L);
        }
    }

    private static void requireExpectedVersion(
            String identitySub,
            long actualVersion,
            long expectedVersion,
            long newVersion) {
        if (actualVersion != expectedVersion || newVersion != Math.addExact(expectedVersion, 1L)) {
            throw stale(identitySub, expectedVersion, actualVersion);
        }
    }

    private static OptimisticLockingFailureException stale(
            String identitySub,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException(
                "identity " + identitySub + " expected version " + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
