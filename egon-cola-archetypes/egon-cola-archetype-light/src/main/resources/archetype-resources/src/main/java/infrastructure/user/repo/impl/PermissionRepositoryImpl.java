package ${package}.infrastructure.user.repo.impl;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.jpa.PermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("permissionRepository")
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final PermissionJpaRepository permissionJpaRepository;
    private final PermissionPOConverter converter;

    @Override
    public Optional<Permission> findByCode(PermissionCode permissionCode) {
        return permissionJpaRepository.findById(permissionCode.value()).map(converter::toDomain);
    }

    @Override
    public Permission save(Permission permission) {
        return converter.toDomain(permissionJpaRepository.save(converter.toPO(permission)));
    }
}
