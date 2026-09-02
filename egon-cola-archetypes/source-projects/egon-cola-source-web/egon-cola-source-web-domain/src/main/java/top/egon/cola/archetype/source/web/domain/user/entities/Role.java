package top.egon.cola.archetype.source.web.domain.user.entities;

import top.egon.cola.archetype.source.web.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;

import java.util.ArrayList;
import java.util.List;

public final class Role {

    private final Long id;
    private final RoleCode code;
    private final String name;
    private final RoleStatus status;
    private final List<PermissionCode> permissionCodes;

    public Role(Long id, RoleCode code, String name, RoleStatus status) {
        this(id, code, name, status, List.of());
    }

    public Role(Long id, RoleCode code, String name, RoleStatus status, List<PermissionCode> permissionCodes) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.permissionCodes = new ArrayList<>(permissionCodes);
    }

    public void grant(PermissionCode permissionCode) { permissionCodes.add(permissionCode); }

    public Long id() { return id; }
    public RoleCode code() { return code; }
    public String name() { return name; }
    public RoleStatus status() { return status; }
    public List<PermissionCode> permissionCodes() { return List.copyOf(permissionCodes); }
}
